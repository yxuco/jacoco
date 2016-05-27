/*******************************************************************************
 * Copyright (c) 2009, 2016 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Evgeny Mandrikov - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.test.validation;

import java.io.IOException;
import java.io.PrintWriter;

import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.SystemPropertiesRuntime;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class ResizeInstructionsTest {

	/**
	 * Triggers {@link org.objectweb.asm.MethodWriter#resizeInstructions()},
	 * which will set {@link ClassWriter#invalidFrames} to {@code true}, so that
	 * {@link ClassWriter#toByteArray()} will set
	 * {@link ClassWriter#computeFrames} to {@code true}. And computation of
	 * frames triggers {@link ClassWriter#getCommonSuperClass(String, String)},
	 * which leads to {@link ClassNotFoundException}.
	 */
	@Test
	public void test() throws IOException {
		byte[] bytes = createClass();
		IRuntime runtime = new SystemPropertiesRuntime();
		Instrumenter instrumenter = new Instrumenter(runtime);
		try {
			instrumenter.instrument(bytes, "Test");
			Assert.fail("IOException expected");
		} catch (IOException e) {
			Assert.assertEquals("java.lang.ClassNotFoundException: Foo",
					e.getCause().getMessage());
		}
	}

	private byte[] createClass() {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
			@Override
			protected String getCommonSuperClass(String type1, String type2) {
				return "java/lang/Object";
			}
		};

		cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "Foo", null,
				"java/lang/Object", null);
		{
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "m", "()V",
					null, null);

			// triggers resizeInstructions :
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitInsn(Opcodes.ICONST_1);
			Label target = new Label();
			mv.visitJumpInsn(Opcodes.IFLE, target);
			for (int i = 0; i < Short.MAX_VALUE; i++) {
				mv.visitInsn(Opcodes.NOP);
			}
			mv.visitLabel(target);

			// from method "testMerge" in
			// "asm/test/conform/org/objectweb/asm/tree/analysis/SimpleVerifierUnitTest.java",
			// triggers getCommonSuperClass :

			Label l0 = new Label();

			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ASTORE, 1);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
			mv.visitVarInsn(Opcodes.ASTORE, 2);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ASTORE, 3);
			mv.visitLabel(l0);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
			mv.visitVarInsn(Opcodes.ASTORE, 1);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ASTORE, 2);
			mv.visitInsn(Opcodes.ACONST_NULL);
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
			mv.visitVarInsn(Opcodes.ASTORE, 3);
			mv.visitJumpInsn(Opcodes.GOTO, l0);

			// mn.visitInsn(Opcodes.RETURN);

			mv.visitMaxs(1, 4);
			mv.visitEnd();
		}

		cw.visitEnd();

		if (false) {
			ClassReader cr = new ClassReader(cw.toByteArray());
			cr.accept(new TraceClassVisitor(null, new ASMifier(),
					new PrintWriter(System.out)), 0);
		}

		return cw.toByteArray();
	}

}
