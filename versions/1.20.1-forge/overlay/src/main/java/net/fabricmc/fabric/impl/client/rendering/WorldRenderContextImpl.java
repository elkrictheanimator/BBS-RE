package net.fabricmc.fabric.impl.client.rendering;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
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

public class WorldRenderContextImpl implements WorldRenderContext
{
    private WorldRenderer worldRenderer;
    private MatrixStack matrixStack;
    private float tickDelta;
    private long limitTime;
    private boolean blockOutlines;
    private Camera camera;
    private Frustum frustum;
    private GameRenderer gameRenderer;
    private LightmapTextureManager lightmapTextureManager;
    private Matrix4f projectionMatrix;
    private ClientWorld world;
    private Profiler profiler;
    private boolean advancedTranslucency;
    private VertexConsumerProvider consumers;

    public void prepare(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, VertexConsumerProvider consumers, Profiler profiler, boolean advancedTranslucency, ClientWorld world)
    {
        this.worldRenderer = worldRenderer;
        this.matrixStack = matrixStack;
        this.tickDelta = tickDelta;
        this.limitTime = limitTime;
        this.blockOutlines = blockOutlines;
        this.camera = camera;
        this.gameRenderer = gameRenderer;
        this.lightmapTextureManager = lightmapTextureManager;
        this.projectionMatrix = projectionMatrix;
        this.consumers = consumers;
        this.profiler = profiler;
        this.advancedTranslucency = advancedTranslucency;
        this.world = world;
    }

    public void setFrustum(Frustum frustum)
    {
        this.frustum = frustum;
    }

    @Override public WorldRenderer worldRenderer() { return this.worldRenderer; }
    @Override public MatrixStack matrixStack() { return this.matrixStack; }
    @Override public float tickDelta() { return this.tickDelta; }
    @Override public long limitTime() { return this.limitTime; }
    @Override public boolean blockOutlines() { return this.blockOutlines; }
    @Override public Camera camera() { return this.camera; }
    @Override public Frustum frustum() { return this.frustum; }
    @Override public GameRenderer gameRenderer() { return this.gameRenderer; }
    @Override public LightmapTextureManager lightmapTextureManager() { return this.lightmapTextureManager; }
    @Override public Matrix4f projectionMatrix() { return this.projectionMatrix; }
    @Override public ClientWorld world() { return this.world; }
    @Override public Profiler profiler() { return this.profiler; }
    @Override public boolean advancedTranslucency() { return this.advancedTranslucency; }
    @Override public VertexConsumerProvider consumers() { return this.consumers; }
}
