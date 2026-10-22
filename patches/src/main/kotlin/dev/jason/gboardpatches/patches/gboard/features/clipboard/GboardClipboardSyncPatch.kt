// Ported from Akshayykadam/PixelBoard (GPL-3.0); see docs/PIXELBOARD_FUSION.md.
package dev.jason.gboardpatches.patches.gboard.features.clipboard

import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.findMutableMethodOrNull
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.gboard.shared.isFieldReference
import dev.jason.gboardpatches.patches.gboard.shared.isMethodReference
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val clipboardOnStartInputViewTargets = listOf(
    GboardMethodTarget(
        classType = "Lfml;",
        name = "ft",
        parameterTypes = listOf(
            "Loyg;",
            "Landroid/view/inputmethod/EditorInfo;",
            "Z",
            "Ljava/util/Map;",
            "Lobf;",
        ),
        returnType = "Z",
    ),
)

internal val gboardClipboardSyncPatch = bytecodePatch(
    description = "Synchronize clipboard history upon input view start for Samsung and Vivo devices.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = clipboardOnStartInputViewTargets.firstNotNullOfOrNull { findMutableMethodOrNull(it) } ?: return@execute
        val implementation = method.implementation ?: return@execute
        val instructions = implementation.instructions
        val call = RuntimeCallId.CLIPBOARD_SYNC_RUNTIME_SYNC_ON_START_INPUT_VIEW
        if (instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
            return@execute
        }

        val igetFIndex = instructions.indexOfFirst { instruction ->
            instruction.isFieldReference("Lfml;->f:Lfmg;") ||
                (instruction.opcode == Opcode.IGET_OBJECT &&
                    ((instruction as? ReferenceInstruction)?.reference as? FieldReference)?.name == "f")
        }
        if (igetFIndex == -1) return@execute

        val igetInstruction = instructions[igetFIndex]
        val handlerReg = if (igetInstruction is TwoRegisterInstruction) {
            "v${igetInstruction.registerA}"
        } else {
            "p4"
        }

        method.addInstructions(
            igetFIndex + 1,
            RuntimeCallEmitter.invoke(call, handlerReg)
        )
    }
}
