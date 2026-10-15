package mchorse.bbs_mod.cubic.model.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loader for a folder holding a binary glTF ({@code .glb}), the format most downloaded cars and
 * props come in.
 *
 * <p>The .glb is converted once into what BBS already knows how to load and edit: a {@code model.obj}
 * with one object per mesh node (so every part is a bone in the pose editor), a {@code model.mtl}, and
 * every material's picture written as {@code textures/<material>/<material>.png}. From there the
 * ordinary OBJ path takes over, which is what gives each part its own texture in the Material tab. The
 * conversion runs again when the .glb is newer than the .obj it made.</p>
 *
 * <p>What is kept: the node hierarchy baked into the vertices at its rest pose, normals, UVs, materials
 * and their base color picture. What is not: skins and animations, and compressed (Draco) meshes.</p>
 */
public class GlbModelLoader implements IModelLoader
{
    private static final String MARKER = "# generated from glb by BBS";

    private static final int GLB_MAGIC = 0x46546C67;
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;

    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        Link glb = null;

        for (Link link : links)
        {
            String path = link.path.toLowerCase(Locale.ROOT);

            if (path.endsWith(".glb") && link.path.startsWith(model.path) && link.path.indexOf('/', model.path.length() + 1) < 0)
            {
                glb = link;

                break;
            }
        }

        if (glb == null)
        {
            return null;
        }

        Link objLink = model.combine("model.obj");
        File objFile = models.provider.getFile(objLink);
        File glbFile = models.provider.getFile(glb);

        if (objFile == null || glbFile == null)
        {
            System.err.println("The .glb model \"" + model + "\" can only be converted from a folder in the assets, not from a pack");

            return null;
        }

        /* An .obj of the user's own (no marker) is theirs; the .glb beside it is left alone */
        if (objFile.isFile() && !this.isGenerated(objFile))
        {
            return null;
        }

        List<Link> all = new ArrayList<>(links);

        if (!objFile.isFile() || objFile.lastModified() < glbFile.lastModified())
        {
            try
            {
                List<Link> made = this.convert(models, model, glb, objFile);

                for (Link link : made)
                {
                    if (!all.contains(link))
                    {
                        all.add(link);
                    }
                }
            }
            catch (Exception e)
            {
                System.err.println("Failed to convert .glb model: " + glb + " (" + e.getMessage() + ")");
                e.printStackTrace();

                return null;
            }
        }
        else if (!all.contains(objLink))
        {
            all.add(objLink);
            all.add(model.combine("model.mtl"));
        }

        ModelInstance instance = new CubicModelLoader().load(id, models, model, all, config);

        /* A model with a single material ignores the material system and wears the model's own texture */
        if (instance != null && instance.baseTexture == null && instance.materials.size() == 1)
        {
            instance.baseTexture = instance.materialTextures.get(instance.materials.get(0));
        }

        return instance;
    }

    private boolean isGenerated(File obj)
    {
        try (InputStream stream = Files.newInputStream(obj.toPath()))
        {
            byte[] head = new byte[MARKER.length()];
            int read = stream.readNBytes(head, 0, head.length);

            return read == head.length && new String(head, StandardCharsets.UTF_8).equals(MARKER);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /** The conversion proper; returns the links of the files it wrote. */
    private List<Link> convert(ModelManager models, Link model, Link glb, File objFile) throws Exception
    {
        byte[] bytes;

        try (InputStream stream = models.provider.getAsset(glb))
        {
            bytes = stream.readAllBytes();
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        if (bytes.length < 20 || buffer.getInt(0) != GLB_MAGIC)
        {
            throw new IllegalStateException("not a binary glTF (.glb) file");
        }

        JsonObject json = null;
        int binOffset = -1;
        int binLength = 0;
        int position = 12;

        while (position + 8 <= bytes.length)
        {
            int length = buffer.getInt(position);
            int type = buffer.getInt(position + 4);
            int start = position + 8;

            if (type == CHUNK_JSON)
            {
                json = JsonParser.parseString(new String(bytes, start, length, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            else if (type == CHUNK_BIN && binOffset < 0)
            {
                binOffset = start;
                binLength = length;
            }

            position = start + length + ((4 - length % 4) % 4);
        }

        if (json == null)
        {
            throw new IllegalStateException("the .glb has no JSON chunk");
        }

        if (json.has("extensionsRequired") && json.get("extensionsRequired").toString().contains("draco"))
        {
            throw new IllegalStateException("Draco-compressed meshes are not supported (export without compression)");
        }

        Glb file = new Glb(json, bytes, binOffset, binLength);
        List<Link> made = new ArrayList<>();
        File modelFolder = objFile.getParentFile();

        modelFolder.mkdirs();

        /* Materials first: names, colours and pictures */
        List<String> names = new ArrayList<>();
        StringBuilder mtl = new StringBuilder(MARKER + "\n");
        JsonArray materials = array(json, "materials");
        Set<String> used = new HashSet<>();

        for (int i = 0; i < Math.max(1, materials.size()); i++)
        {
            JsonObject material = i < materials.size() ? materials.get(i).getAsJsonObject() : new JsonObject();
            String name = unique(used, material.has("name") ? material.get("name").getAsString() : "material_" + i, "material_" + i);
            float[] color = {1F, 1F, 1F};
            JsonObject pbr = material.has("pbrMetallicRoughness") ? material.getAsJsonObject("pbrMetallicRoughness") : null;

            names.add(name);
            mtl.append("newmtl ").append(name).append('\n');

            if (pbr != null && pbr.has("baseColorFactor"))
            {
                JsonArray factor = pbr.getAsJsonArray("baseColorFactor");

                for (int c = 0; c < 3; c++)
                {
                    color[c] = factor.get(c).getAsFloat();
                }
            }

            mtl.append("Kd ").append(color[0]).append(' ').append(color[1]).append(' ').append(color[2]).append('\n');

            if (pbr != null && pbr.has("baseColorTexture"))
            {
                int textureIndex = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
                byte[] png = file.texturePng(textureIndex);

                if (png != null)
                {
                    File folder = new File(modelFolder, "textures/" + name);

                    folder.mkdirs();
                    Files.write(new File(folder, name + ".png").toPath(), png);
                    made.add(model.combine("textures/" + name + "/" + name + ".png"));
                    mtl.append("map_Kd ").append(name).append(".png\n");
                }
            }

            mtl.append('\n');
        }

        /* Geometry: every node that carries a mesh becomes one object, baked into the world of its rest pose */
        StringBuilder obj = new StringBuilder(MARKER + "\nmtllib model.mtl\n");
        Counts counts = new Counts();
        Set<String> objectNames = new HashSet<>();
        JsonArray scenes = array(json, "scenes");
        JsonArray roots = scenes.size() > 0
            ? scenes.get(json.has("scene") ? Math.min(json.get("scene").getAsInt(), scenes.size() - 1) : 0).getAsJsonObject().getAsJsonArray("nodes")
            : null;

        if (roots == null)
        {
            roots = new JsonArray();

            for (int i = 0; i < array(json, "nodes").size(); i++)
            {
                roots.add(i);
            }
        }

        for (JsonElement root : roots)
        {
            this.walk(file, root.getAsInt(), new Matrix4f(), obj, counts, names, objectNames, 0);
        }

        if (counts.faces == 0)
        {
            throw new IllegalStateException("the .glb holds no triangles");
        }

        Files.write(objFile.toPath(), obj.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(new File(modelFolder, "model.mtl").toPath(), mtl.toString().getBytes(StandardCharsets.UTF_8));
        made.add(model.combine("model.obj"));
        made.add(model.combine("model.mtl"));

        return made;
    }

    private void walk(Glb file, int index, Matrix4f parent, StringBuilder obj, Counts counts, List<String> materialNames, Set<String> objectNames, int depth)
    {
        JsonArray nodes = array(file.json, "nodes");

        if (index < 0 || index >= nodes.size() || depth > 64)
        {
            return;
        }

        JsonObject node = nodes.get(index).getAsJsonObject();
        Matrix4f world = new Matrix4f(parent).mul(local(node));

        if (node.has("mesh"))
        {
            JsonArray meshes = array(file.json, "meshes");
            int meshIndex = node.get("mesh").getAsInt();

            if (meshIndex >= 0 && meshIndex < meshes.size())
            {
                String fallback = "part_" + index;
                String name = unique(objectNames, node.has("name") ? node.get("name").getAsString() : fallback, fallback);

                obj.append("o ").append(name).append('\n');
                this.writeMesh(file, meshes.get(meshIndex).getAsJsonObject(), world, obj, counts, materialNames);
            }
        }

        if (node.has("children"))
        {
            for (JsonElement child : node.getAsJsonArray("children"))
            {
                this.walk(file, child.getAsInt(), world, obj, counts, materialNames, objectNames, depth + 1);
            }
        }
    }

    private void writeMesh(Glb file, JsonObject mesh, Matrix4f world, StringBuilder obj, Counts counts, List<String> materialNames)
    {
        Matrix3f normalMatrix = new Matrix3f();

        world.normal(normalMatrix);

        for (JsonElement element : array(mesh, "primitives"))
        {
            JsonObject primitive = element.getAsJsonObject();
            int mode = primitive.has("mode") ? primitive.get("mode").getAsInt() : 4;

            if (mode != 4 || !primitive.has("attributes"))
            {
                continue;
            }

            JsonObject attributes = primitive.getAsJsonObject("attributes");

            if (!attributes.has("POSITION"))
            {
                continue;
            }

            float[] positions = file.floats(attributes.get("POSITION").getAsInt(), 3);
            float[] normals = attributes.has("NORMAL") ? file.floats(attributes.get("NORMAL").getAsInt(), 3) : null;
            float[] uvs = attributes.has("TEXCOORD_0") ? file.floats(attributes.get("TEXCOORD_0").getAsInt(), 2) : null;
            int vertices = positions.length / 3;
            int[] indices;

            if (primitive.has("indices"))
            {
                indices = file.indices(primitive.get("indices").getAsInt());
            }
            else
            {
                indices = new int[vertices];

                for (int i = 0; i < vertices; i++)
                {
                    indices[i] = i;
                }
            }

            int material = primitive.has("material") ? primitive.get("material").getAsInt() : 0;

            obj.append("usemtl ").append(materialNames.get(Math.max(0, Math.min(material, materialNames.size() - 1)))).append('\n');

            Vector3f vector = new Vector3f();

            for (int i = 0; i < vertices; i++)
            {
                world.transformPosition(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2], vector);
                obj.append("v ").append(vector.x).append(' ').append(vector.y).append(' ').append(vector.z).append('\n');
            }

            for (int i = 0; i < vertices; i++)
            {
                float u = uvs != null && i * 2 + 1 < uvs.length ? uvs[i * 2] : 0F;
                float v = uvs != null && i * 2 + 1 < uvs.length ? 1F - uvs[i * 2 + 1] : 0F;

                obj.append("vt ").append(u).append(' ').append(v).append('\n');
            }

            for (int i = 0; i < vertices; i++)
            {
                if (normals != null && i * 3 + 2 < normals.length)
                {
                    normalMatrix.transform(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2], vector);
                    vector.normalize();
                }
                else
                {
                    vector.set(0F, 1F, 0F);
                }

                obj.append("vn ").append(vector.x).append(' ').append(vector.y).append(' ').append(vector.z).append('\n');
            }

            for (int i = 0; i + 2 < indices.length; i += 3)
            {
                obj.append('f');

                for (int k = 0; k < 3; k++)
                {
                    int n = counts.vertices + indices[i + k] + 1;

                    obj.append(' ').append(n).append('/').append(n).append('/').append(n);
                }

                obj.append('\n');
                counts.faces++;
            }

            counts.vertices += vertices;
        }
    }

    private static Matrix4f local(JsonObject node)
    {
        Matrix4f matrix = new Matrix4f();

        if (node.has("matrix"))
        {
            JsonArray m = node.getAsJsonArray("matrix");
            float[] values = new float[16];

            for (int i = 0; i < 16 && i < m.size(); i++)
            {
                values[i] = m.get(i).getAsFloat();
            }

            return matrix.set(values);
        }

        if (node.has("translation"))
        {
            JsonArray t = node.getAsJsonArray("translation");

            matrix.translate(t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat());
        }

        if (node.has("rotation"))
        {
            JsonArray r = node.getAsJsonArray("rotation");

            matrix.rotate(new Quaternionf(r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat(), r.get(3).getAsFloat()).normalize());
        }

        if (node.has("scale"))
        {
            JsonArray s = node.getAsJsonArray("scale");

            matrix.scale(s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat());
        }

        return matrix;
    }

    private static JsonArray array(JsonObject object, String key)
    {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }

    /** A name an .obj / folder can carry: no spaces or slashes, and not one already taken. */
    private static String unique(Set<String> used, String wanted, String fallback)
    {
        String name = wanted.trim().replaceAll("[^A-Za-z0-9_.\\-]", "_");

        if (name.isEmpty())
        {
            name = fallback;
        }

        String result = name;
        int i = 2;

        while (!used.add(result))
        {
            result = name + "_" + i;
            i += 1;
        }

        return result;
    }

    private static class Counts
    {
        public int vertices;
        public int faces;
    }

    /** The binary buffer of a .glb and the accessors that read it. */
    private static class Glb
    {
        public final JsonObject json;
        public final ByteBuffer data;

        private final Map<Integer, byte[]> pngs = new HashMap<>();

        public Glb(JsonObject json, byte[] bytes, int binOffset, int binLength)
        {
            this.json = json;
            this.data = binOffset < 0
                ? ByteBuffer.allocate(0).order(ByteOrder.LITTLE_ENDIAN)
                : ByteBuffer.wrap(bytes, binOffset, binLength).slice().order(ByteOrder.LITTLE_ENDIAN);
        }

        private int components(String type)
        {
            switch (type)
            {
                case "SCALAR": return 1;
                case "VEC2": return 2;
                case "VEC3": return 3;
                case "VEC4": return 4;
                default: return 1;
            }
        }

        /** Where the accessor's element {@code i} starts in the buffer, and the bytes between elements. */
        private float[] read(int accessorIndex, int wanted)
        {
            JsonObject accessor = array(this.json, "accessors").get(accessorIndex).getAsJsonObject();
            int count = accessor.get("count").getAsInt();
            int componentType = accessor.get("componentType").getAsInt();
            int components = this.components(accessor.get("type").getAsString());
            boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();
            float[] out = new float[count * wanted];

            if (!accessor.has("bufferView"))
            {
                return out;
            }

            JsonObject view = array(this.json, "bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
            int base = (view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0)
                + (accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0);
            int size = componentSize(componentType);
            int stride = view.has("byteStride") && view.get("byteStride").getAsInt() > 0 ? view.get("byteStride").getAsInt() : size * components;

            for (int i = 0; i < count; i++)
            {
                for (int c = 0; c < Math.min(wanted, components); c++)
                {
                    out[i * wanted + c] = this.component(base + i * stride + c * size, componentType, normalized);
                }
            }

            return out;
        }

        public float[] floats(int accessorIndex, int wanted)
        {
            return this.read(accessorIndex, wanted);
        }

        public int[] indices(int accessorIndex)
        {
            JsonObject accessor = array(this.json, "accessors").get(accessorIndex).getAsJsonObject();
            int count = accessor.get("count").getAsInt();
            int componentType = accessor.get("componentType").getAsInt();
            JsonObject view = array(this.json, "bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
            int base = (view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0)
                + (accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0);
            int size = componentSize(componentType);
            int stride = view.has("byteStride") && view.get("byteStride").getAsInt() > 0 ? view.get("byteStride").getAsInt() : size;
            int[] out = new int[count];

            for (int i = 0; i < count; i++)
            {
                int at = base + i * stride;

                switch (componentType)
                {
                    case 5121: out[i] = this.data.get(at) & 0xFF; break;
                    case 5123: out[i] = this.data.getShort(at) & 0xFFFF; break;
                    default: out[i] = this.data.getInt(at); break;
                }
            }

            return out;
        }

        private float component(int at, int type, boolean normalized)
        {
            switch (type)
            {
                case 5126: return this.data.getFloat(at);
                case 5120: return normalized ? Math.max(this.data.get(at) / 127F, -1F) : this.data.get(at);
                case 5121: return normalized ? (this.data.get(at) & 0xFF) / 255F : this.data.get(at) & 0xFF;
                case 5122: return normalized ? Math.max(this.data.getShort(at) / 32767F, -1F) : this.data.getShort(at);
                case 5123: return normalized ? (this.data.getShort(at) & 0xFFFF) / 65535F : this.data.getShort(at) & 0xFFFF;
                default: return this.data.getInt(at);
            }
        }

        /** The picture of a glTF texture as PNG bytes (a .jpg inside is converted), or null when it has none. */
        public byte[] texturePng(int textureIndex) throws Exception
        {
            if (this.pngs.containsKey(textureIndex))
            {
                return this.pngs.get(textureIndex);
            }

            byte[] result = null;
            JsonArray textures = array(this.json, "textures");

            if (textureIndex >= 0 && textureIndex < textures.size())
            {
                JsonObject texture = textures.get(textureIndex).getAsJsonObject();
                JsonArray images = array(this.json, "images");

                if (texture.has("source") && texture.get("source").getAsInt() < images.size())
                {
                    JsonObject image = images.get(texture.get("source").getAsInt()).getAsJsonObject();

                    if (image.has("bufferView"))
                    {
                        JsonObject view = array(this.json, "bufferViews").get(image.get("bufferView").getAsInt()).getAsJsonObject();
                        int offset = view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0;
                        byte[] raw = new byte[view.get("byteLength").getAsInt()];

                        for (int i = 0; i < raw.length; i++)
                        {
                            raw[i] = this.data.get(offset + i);
                        }

                        boolean isPng = raw.length > 4 && (raw[0] & 0xFF) == 0x89 && raw[1] == 'P' && raw[2] == 'N' && raw[3] == 'G';

                        if (isPng)
                        {
                            result = raw;
                        }
                        else
                        {
                            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(raw));

                            if (decoded != null)
                            {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();

                                ImageIO.write(decoded, "png", out);
                                result = out.toByteArray();
                            }
                        }
                    }
                }
            }

            this.pngs.put(textureIndex, result);

            return result;
        }

        private static int componentSize(int type)
        {
            switch (type)
            {
                case 5120:
                case 5121:
                    return 1;
                case 5122:
                case 5123:
                    return 2;
                default:
                    return 4;
            }
        }
    }
}
