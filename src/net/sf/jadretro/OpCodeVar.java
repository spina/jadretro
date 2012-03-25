/*
 * @(#) src/net/sf/jadretro/OpCodeVar.java --
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

final class OpCodeVar extends OpByteCode {

	private static final int ALOAD = 0x19;

	private static final int ALOAD_3 = 0x2d;

	private static final int ASTORE = 0x3a;

	private static final int ASTORE_3 = 0x4e;

	private static final int IINC = 0x84;

	private static final int ILOAD = 0x15;

	private static final int ILOAD_0 = 0x1a;

	private static final int ISTORE = 0x36;

	private static final int ISTORE_0 = 0x3b;

	private static final int RET = 0xa9;

	private static final int WIDE = 0xc4;

	private/* final */int op;

	private/* final */int type;

	private/* final */int index;

	private/* final */int incValue;

	private OpCodeVar(int op, int type, int index, int incValue) {
		this.op = op;
		this.type = type;
		this.index = index;
		this.incValue = incValue;
	}

	private OpCodeVar(int type, int index) {
		op = index > 0xff ? WIDE : (index & ~3) != 0 ? type : type >= ILOAD
				&& type <= ALOAD ? ((type - ILOAD) << 2) + index + ILOAD_0
				: type >= ISTORE && type <= ASTORE ? ((type - ISTORE) << 2)
						+ index + ISTORE_0 : type;
		this.type = type;
		this.index = index;
		incValue = 0;
	}

	static OpCodeVar decode(int op, InputStream in) throws IOException {
		if (op >= ILOAD_0 && op <= ALOAD_3)
			return new OpCodeVar(op, ILOAD + ((op - ILOAD_0) >> 2),
					(op - ILOAD_0) & 0x3, 0);
		if (op >= ISTORE_0 && op <= ASTORE_3)
			return new OpCodeVar(op, ISTORE + ((op - ISTORE_0) >> 2),
					(op - ISTORE_0) & 0x3, 0);
		int type = op;
		if (op == WIDE) {
			type = readUnsignedByte(in);
		}
		if ((type >= ILOAD && type <= ALOAD)
				|| (type >= ISTORE && type <= ASTORE) || type == IINC
				|| type == RET) {
			int index = op == WIDE ? readUnsignedShort(in)
					: readUnsignedByte(in);
			return new OpCodeVar(op, type, index,
					type == IINC ? (op == WIDE ? readShort(in) : readByte(in))
							: 0);
		}
		if (op == WIDE)
			throw new BadClassFileException();
		return null;
	}

	static OpCodeVar makeAstoreAload(int varInd, boolean isStore) {
		return new OpCodeVar(isStore ? ASTORE : ALOAD, varInd);
	}

	static OpCodeVar makeRet(int varInd) {
		return new OpCodeVar(RET, varInd);
	}

	int getLength(int curPc) {
		return op == WIDE ? (type == IINC ? 6 : 4) : type == IINC ? 3
				: (op < ILOAD_0 || op > ALOAD_3)
						&& (op < ISTORE_0 || op > ASTORE_3) ? 2 : 1;
	}

	void writeTo(OutputStream out) throws IOException {
		out.write(op);
		if (op == WIDE) {
			out.write(type);
			writeShort(out, index);
			if (type == IINC) {
				writeShort(out, incValue);
			}
		} else if ((op < ILOAD_0 || op > ALOAD_3)
				&& (op < ISTORE_0 || op > ASTORE_3)) {
			out.write(index);
			if (type == IINC) {
				out.write(incValue);
			}
		}
	}

	boolean isEqualTo(OpByteCode other, int startIndex, int deltaIndex,
			int endIndex2, int[] indexRef, int[] deltaVarRef, int argSlots,
			Hashtable diffVarsSet) {
		if (!(other instanceof OpCodeVar))
			return false;
		OpCodeVar opCodeVar = (OpCodeVar) other;
		if (type != opCodeVar.type || incValue != opCodeVar.incValue)
			return false;
		int varInd = getVarIndex();
		int diff = opCodeVar.getVarIndex() - varInd;
		if (diff != 0) {
			if (deltaVarRef[0] != diff) {
				if (deltaVarRef[0] != 0)
					return false;
				deltaVarRef[0] = diff;
			}
			if (varInd < argSlots || varInd + diff < argSlots)
				return false;
			if (type >= ISTORE && type <= ASTORE) {
				diffVarsSet.put(new Integer(varInd), "");
			} else if (diffVarsSet.get(new Integer(varInd)) == null)
				return false;
		}
		return true;
	}

	boolean isAstoreAload(boolean isStore) {
		return isStore ? type == ASTORE : type == ALOAD;
	}

	boolean isRetXLoad(boolean isRet) {
		return isRet ? type == RET : type >= ILOAD && type <= ALOAD;
	}

	int getVarIndex() {
		return index;
	}

	boolean isUncondBranch() {
		return type == RET;
	}
}
