/*
 * @(#) net/sf/jadretro/OpByteCode.java --
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

abstract class OpByteCode extends ClassLabeledEntity {

	abstract int getLength(int curPc);

	void writeTo(OutputStream out) throws IOException {
		throw new IllegalArgumentException();
	}

	void writeRelTo(OutputStream out, int curPc) throws IOException {
		writeTo(out);
	}

	abstract boolean isEqualTo(OpByteCode other, int startIndex,
			int deltaIndex, int endIndex2, int[] indexRef, int[] deltaVarRef,
			int argSlots, Hashtable diffVarsSet);

	boolean isAstoreAload(boolean isStore) {
		return false;
	}

	boolean isAthrow() {
		return false;
	}

	boolean isCheckcast() {
		return false;
	}

	boolean isInvokeMethod() {
		return false;
	}

	boolean isInvokestaticSpecial(boolean isStatic) {
		return false;
	}

	boolean isInvokevirtual() {
		return false;
	}

	boolean isJsrGoto(boolean isJsr) {
		return false;
	}

	boolean isLdc() {
		return false;
	}

	boolean isMonitorexit() {
		return false;
	}

	boolean isPutGetstatic(boolean isPut) {
		return false;
	}

	boolean isPutfield() {
		return false;
	}

	boolean isRetXLoad(boolean isRet) {
		return false;
	}

	boolean isXConstZero() {
		return false;
	}

	boolean isXReturn() {
		return false;
	}

	int getVarIndex() {
		return -1;
	}

	ConstantRef getConstRef() {
		return null;
	}

	CodeAbsLabel getTargetLabel() {
		return null;
	}

	boolean isTargetInRange(int startIndex, int endIndex) {
		return false;
	}

	boolean isUncondBranch() {
		return false;
	}
}
