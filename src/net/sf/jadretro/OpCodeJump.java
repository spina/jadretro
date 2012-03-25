/*
 * @(#) src/net/sf/jadretro/OpCodeJump.java --
 * a part of JadRetro source.
 **
 * Copyright (C) 2007-2008 Ivan Maidanski <ivmai@mail.ru>
 * All rights reserved.
 */

/*
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 **
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License (GPL) for more details.
 **
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 **
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package net.sf.jadretro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

final class OpCodeJump extends OpByteCode {

	private static final int GOTO = 0xa7;

	private static final int GOTO_W = 0xc8;

	private static final int IFEQ = 0x99;

	private static final int IFNONNULL = 0xc7;

	private static final int IFNULL = 0xc6;

	private static final int JSR = 0xa8;

	private static final int JSR_W = 0xc9;

	private/* final */int op;

	private/* final */CodeAbsLabel targetLabel;

	private OpCodeJump(int op, CodeAbsLabel targetLabel) {
		this.op = op;
		this.targetLabel = targetLabel;
	}

	static OpCodeJump decode(int op, InputStream in, int curPc)
			throws IOException {
		return op == GOTO_W || op == JSR_W ? new OpCodeJump(op,
				new CodeAbsLabel(in, true, curPc)) : (op >= IFEQ && op <= JSR)
				|| op == IFNULL || op == IFNONNULL ? new OpCodeJump(op,
				new CodeAbsLabel(in, false, curPc)) : null;
	}

	static OpCodeJump makeIfnonnull(int targetIndex) {
		CodeAbsLabel targetLabel = new CodeAbsLabel();
		targetLabel.setNewIndex(targetIndex);
		return new OpCodeJump(IFNONNULL, targetLabel);
	}

	static OpCodeJump makeJsrGoto(int targetIndex, boolean isJsr) {
		CodeAbsLabel targetLabel = new CodeAbsLabel();
		targetLabel.setNewIndex(targetIndex);
		return new OpCodeJump(isJsr ? JSR : GOTO, targetLabel);
	}

	void mapLabelsPc(int[] indices) throws BadClassFileException {
		targetLabel.mapLabelsPc(indices);
	}

	void incLabelIndices(int startIndex, int incValue) {
		targetLabel.incLabelIndices(startIndex, incValue);
	}

	void rebuildLabelsPc(int[] offsets) {
		targetLabel.rebuildLabelsPc(offsets);
	}

	int getLength(int curPc) {
		return op == GOTO_W || op == JSR_W ? 5 : 3;
	}

	void writeRelTo(OutputStream out, int curPc) throws IOException {
		out.write(op);
		targetLabel.writeRelTo(out, op == GOTO_W || op == JSR_W, curPc);
	}

	boolean isEqualTo(OpByteCode other, int startIndex, int deltaIndex,
			int endIndex2, int[] indexRef, int[] deltaVarRef, int argSlots,
			Hashtable diffVarsSet) {
		if (!(other instanceof OpCodeJump))
			return false;
		OpCodeJump opCodeJump = (OpCodeJump) other;
		return op == opCodeJump.op
				&& targetLabel.isEqualTo(opCodeJump.targetLabel, startIndex,
						deltaIndex, endIndex2, indexRef);
	}

	boolean isJsrGoto(boolean isJsr) {
		return isJsr ? op == JSR || op == JSR_W : op == GOTO || op == GOTO_W;
	}

	CodeAbsLabel getTargetLabel() {
		return targetLabel;
	}

	boolean isTargetInRange(int startIndex, int endIndex) {
		return targetLabel.isInRange(startIndex, endIndex);
	}

	boolean isUncondBranch() {
		return op == GOTO || op == GOTO_W;
	}
}
