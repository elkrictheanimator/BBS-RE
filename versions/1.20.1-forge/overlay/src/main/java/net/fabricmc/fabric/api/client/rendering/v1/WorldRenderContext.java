package net.fabricmc.fabric.api.client.rendering.v1;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;

public interface WorldRenderContext
{
    WorldRenderer worldRenderer();

    MatrixStack matrixStack();

    float tickDelta();

    long limitTime();

    boolean blockOutlines();

    Camera camera();

    Frustum frustum();

    GameRenderer gameRenderer();

    LightmapTextureManager lightmapTextureManager();

    Matrix4f projectionMatrix();

    ClientWorld world();

    Profiler profiler();

    boolean advancedTranslucency();

    VertexConsumerProvider consumers();
}
