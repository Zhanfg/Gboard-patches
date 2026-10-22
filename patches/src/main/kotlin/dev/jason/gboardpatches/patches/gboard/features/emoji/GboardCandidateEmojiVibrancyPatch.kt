// Ported from Akshayykadam/PixelBoard (GPL-3.0); see docs/PIXELBOARD_FUSION.md.
package dev.jason.gboardpatches.patches.gboard.features.emoji

import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.findMutableMethodOrNull
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.gboard.shared.isMethodReference
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private val autoCenterScaleTextViewOnDrawTarget = GboardMethodTarget(
    classType = "Lcom/google/android/libraries/inputmethod/widgets/text/AutoCenterScaleTextView;",
    name = "onDraw",
    parameterTypes = listOf("Landroid/graphics/Canvas;"),
    returnType = "V",
)

internal val gboardCandidateEmojiVibrancyPatch = bytecodePatch(
    description = "Ensure candidate strip emojis render with full opacity and vibrant colors.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = findMutableMethodOrNull(autoCenterScaleTextViewOnDrawTarget) ?: return@execute
        val implementation = method.implementation ?: return@execute
        val instructions = implementation.instructions
        val call = RuntimeCallId.CANDIDATE_EMOJI_RUNTIME_ADJUST_CANDIDATE_PAINT
        if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }

        val setColorIndex = instructions.indexOfFirst { instruction ->
            instruction.isMethodReference("Landroid/text/TextPaint;->setColor(I)V") ||
                instruction.isMethodReference("Landroid/graphics/Paint;->setColor(I)V")
        }
        if (setColorIndex == -1) return@execute

        val setColorInstruction = instructions[setColorIndex]
        val paintReg = if (setColorInstruction is FiveRegisterInstruction) {
            "v${setColorInstruction.registerC}"
        } else {
            "v0"
        }

        method.addInstructions(
            setColorIndex + 1,
            RuntimeCallEmitter.invoke(call, "p0, $paintReg")
        )
    }
}
