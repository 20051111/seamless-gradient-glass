package com.ixce.gradientglass.mixin;

import com.ixce.gradientglass.BeamTransition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BeamEmitter.BeamSegment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeaconBlockEntityRenderer.class)
abstract class BeaconRendererMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/block/entity/BeaconBlockEntityRenderer;renderBeam(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;FFJIII)V"))
    private void gradientglass$segment(MatrixStack matrices, VertexConsumerProvider buffers, float tickDelta,
            float scale, long time, int y, int height, int color, Operation<Void> draw,
            @Local(argsOnly = true) BlockEntity entity, @Local(index = 13) List<BeamSegment> segments,
            @Local(index = 15) int index) {
        if (entity instanceof BeaconBlockEntity && index > 0) {
            int previous = segments.get(index - 1).getColor();
            if (previous != color) buffers = new BeamTransition(buffers, previous);
        }
        draw.call(matrices, buffers, tickDelta, scale, time, y, height, color);
    }

    @WrapOperation(method = "renderBeam(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/util/Identifier;FFJIIIFF)V",
        at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/block/entity/BeaconBlockEntityRenderer;renderBeamLayer(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIIFFFFFFFFFFFF)V"), require = 2)
    private static void gradientglass$layer(MatrixStack matrices, VertexConsumer vertices, int color, int bottom,
            int top, float x1, float z1, float x2, float z2, float x3, float z3, float x4, float z4,
            float u0, float u1, float topV, float bottomV, Operation<Void> draw,
            @Local(argsOnly = true) VertexConsumerProvider buffers) {
        if (buffers instanceof BeamTransition transition && top > bottom) {
            int lowerColor = ColorHelper.withAlpha(color >>> 24, transition.lowerColor());
            int end = bottom + 1;
            float endV = bottomV + (topV - bottomV) / (top - bottom);
            var matrix = matrices.peek();
            gradientglass$face(matrix, vertices, bottom, end, lowerColor, color, x1, z1, x2, z2, u0, u1, bottomV, endV);
            gradientglass$face(matrix, vertices, bottom, end, lowerColor, color, x4, z4, x3, z3, u0, u1, bottomV, endV);
            gradientglass$face(matrix, vertices, bottom, end, lowerColor, color, x2, z2, x4, z4, u0, u1, bottomV, endV);
            gradientglass$face(matrix, vertices, bottom, end, lowerColor, color, x3, z3, x1, z1, u0, u1, bottomV, endV);
            if (end == top) return;
            bottom = end;
            bottomV = endV;
        }
        draw.call(matrices, vertices, color, bottom, top, x1, z1, x2, z2, x3, z3, x4, z4, u0, u1, topV, bottomV);
    }

    @Unique
    private static void gradientglass$face(MatrixStack.Entry matrix, VertexConsumer vertices, int bottom, int top,
            int lower, int upper, float x1, float z1, float x2, float z2, float u0, float u1, float bottomV, float topV) {
        renderBeamVertex(matrix, vertices, upper, top, x1, z1, u1, topV);
        renderBeamVertex(matrix, vertices, lower, bottom, x1, z1, u1, bottomV);
        renderBeamVertex(matrix, vertices, lower, bottom, x2, z2, u0, bottomV);
        renderBeamVertex(matrix, vertices, upper, top, x2, z2, u0, topV);
    }

    @Shadow
    private static void renderBeamVertex(MatrixStack.Entry matrix, VertexConsumer vertices, int color, int y,
            float x, float z, float u, float v) { throw new AssertionError(); }
}
