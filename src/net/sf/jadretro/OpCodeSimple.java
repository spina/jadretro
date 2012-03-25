/*
 * @(#) src/net/sf/jadretro/OpCodeSimple.java --
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
import java.io.OutputStream;
import java.util.Hashtable;

final class OpCodeSimple extends OpByteCode {

	private static final int ACONST_NULL = 0x1;

	private static final int ARETURN = 0xb0;

	private static final int ATHROW = 0xbf;

	private static final int DCONST_0 = 0xe;

	private static final int DUP = 0x59;

	private static final int FCONST_0 = 0xb;

	private static final int ICONST_0 = 0x3;

	private static final int IRETURN = 0xac;

	private static final int LCONST_0 = 0x9;

	private static final int MONITOREXIT = 0xc3;

	private static final int RETURN = 0xb1;

	private/* final */int op;

	OpCodeSimple(int op) {
		this.op = op;
	}

	static OpCodeSimple makeAreturn() {
		return new OpCodeSimple(ARETURN);
	}

	static OpCodeSimple makeAthrow() {
		return new OpCodeSimple(ATHROW);
	}

	static OpCodeSimple makeDup() {
		return new OpCodeSimple(DUP);
	}

	static OpCodeSimple makeIconstZero() {
		return new OpCodeSimple(ICONST_0);
	}

	static OpCodeSimple makeMonitorexit() {
		return new OpCodeSimple(MONITOREXIT);
	}

	int getLength(int curPc) {
		return 1;
	}

	void writeTo(OutputStream out) throws IOException {
		out.write(op);
	}

	boolean isEqualTo(OpByteCode other, int startIndex, int deltaIndex,
			int endIndex2, int[] indexRef, int[] deltaVarRef, int argSlots,
			Hashtable diffVarsSet) {
		if (!(other instanceof OpCodeSimple))
			return false;
		return ((OpCodeSimple) other).op == op;
	}

	boolean isAthrow() {
		return op == ATHROW;
	}

	boolean isMonitorexit() {
		return op == MONITOREXIT;
	}

	boolean isXConstZero() {
		return op == ACONST_NULL || op == ICONST_0 || op == LCONST_0
				|| op == FCONST_0 || op == DCONST_0;
	}

	boolean isXReturn() {
		return op >= IRETURN && op <= RETURN;
	}

	boolean isUncondBranch() {
		return op == ATHROW || (op >= IRETURN && op <= RETURN);
	}
}
