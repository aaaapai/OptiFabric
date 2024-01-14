package me.modmuss50.optifabric.patcher.fixes;

import java.util.ListIterator;

import me.modmuss50.optifabric.mod.OptifabricSetup;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.modmuss50.optifabric.util.RemappingUtils;

public class ChunkRendererFix implements ClassFixer {

	//This removes the small change that optifine made to ChunkRenderer that is only required by forge
	@Override
	public void fix(ClassNode classNode, ClassNode old) {
		for (MethodNode methodNode : classNode.methods) {
			for (ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator(); it.hasNext();) {
				AbstractInsnNode insnNode = it.next();

				if (insnNode instanceof MethodInsnNode) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;

					if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
						if ("renderModel".equals(methodInsnNode.name) || "renderBatched".equals(methodInsnNode.name)) {
							/* For reference:

							class_2680 - net/minecraft/block/BlockState
							class_2338 - net/minecraft/util/math/BlockPos
							class_1920 - net/minecraft/world/ExtendedBlockView
							class_4587 - net/minecraft/client/util/math/MatrixStack
							class_4588 - net/minecraft/client/render/VertexConsumer
							class_5819 - net/minecraft/util/math/random/Random
							 */

							Type[] args = Type.getArgumentTypes(methodInsnNode.desc);
							int end = args.length - 1;
							boolean trailingBoolean = Type.BOOLEAN == args[end].getSort();
							if (trailingBoolean) end--;
							boolean trailingRenderLayer = RemappingUtils.getClassName("class_1921").equals(args[end].getInternalName());
							if (trailingRenderLayer) end--;
							assert "net/minecraftforge/client/model/data/IModelData".equals(args[end].getInternalName());
							end--;
							assert end > 0 && Type.BOOLEAN == args[end - 1].getSort();
							boolean nativeRandom = "java/util/Random".equals(args[end].getInternalName());
							String desc = "(Lnet/minecraft/class_2680;"
								+ "Lnet/minecraft/class_2338;"
								+ "Lnet/minecraft/class_1920;"
								+ "Lnet/minecraft/class_4587;"
								+ "Lnet/minecraft/class_4588;"
								+ "ZL" + (!nativeRandom ? "net/minecraft/class_5819;)V" : "java/util/Random;)Z");
							String name = RemappingUtils.getMethodName("class_776", "method_3355", desc);

							OptifabricSetup.LOGGER.info("Replacement `renderBlock` call:  {}.{}", name, desc);
	
							//Replaces the method call with the vanilla one, this calls down to the same method just without the forge model data
							methodInsnNode.name = name;
							methodInsnNode.desc = RemappingUtils.mapMethodDescriptor(desc);

							it.previous();
							if (trailingBoolean) {//Remove the additional boolean
								it.previous();
								it.remove();
							}
							//Remove the model data local load call
							it.previous();
							it.remove();
							if (trailingRenderLayer) {//Remove the additional render layer
								it.previous();
								it.remove();
							}
							it.next();
						}
					}
				}
			}
		}
	}
}
