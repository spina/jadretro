/*
 * @(#) net/sf/jadretro/CodeAbsLabel.java --
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

final class CodeAbsLabel extends ClassLabeledEntity {

	private int targetPc;

	private int index = -1;

	CodeAbsLabel() {
	}

	CodeAbsLabel(InputStream in, CodeAbsLabel other) throws IOException {
		targetPc = readUnsignedShort(in) + (other != null ? other.getPc() : 0);
	}

	CodeAbsLabel(InputStream in, boolean isWide, int curPc) throws IOException {
		if ((targetPc = (isWide ? readInt(in) : readShort(in)) + curPc) < 0)
			throw new BadClassFileException();
	}

	void mapLabelsPc(int[] indices) throws BadClassFileException {
		mapLabelsPc(indices, false);
	}

	void mapLabelsPc(int[] indices, boolean isMaxAllowed)
			throws BadClassFileException {
		if (targetPc >= 0) {
			if (indices.length - (isMaxAllowed ? 0 : 1) <= targetPc
					|| (index = indices[targetPc]) < 0)
				throw new BadClassFileException();
			targetPc = -1;
		}
	}

	void incLabelIndices(int startIndex, int incValue) {
		if (index >= startIndex) {
			setNewIndex(index + incValue);
		}
	}

	void rebuildLabelsPc(int[] offsets) {
		if (index < 0 || index >= offsets.length)
			throw new IllegalArgumentException();
		targetPc = offsets[index];
	}

	void writeTo(OutputStream out) throws IOException {
		writeRelTo(out, null);
	}

	void writeRelTo(OutputStream out, CodeAbsLabel other) throws IOException {
		writeCheckedUShort(out, getPc() - (other != null ? other.getPc() : 0));
	}

	void writeRelTo(OutputStream out, boolean isWide, int curPc)
			throws IOException {
		if (isWide) {
			writeInt(out, getPc() - curPc);
		} else {
			int value = getPc() - curPc;
			if (((short) value) != value)
				throw new ClassOverflowException();
			writeShort(out, value);
		}
	}

	private int getPc() {
		return targetPc;
	}

	boolean isEqualTo(CodeAbsLabel other, int startIndex, int deltaIndex,
			int endIndex2, int[] indexRef) {
		int diff = other.index - index;
		if (diff != 0) {
			if (other.index != endIndex2)
				return diff == deltaIndex && index >= startIndex
						&& other.index < endIndex2;
			if (indexRef[0] != index) {
				if (indexRef[0] >= 0)
					return false;
				indexRef[0] = index;
			}
		}
		return true;
	}

	boolean isInRange(int startIndex, int endIndex) {
		return index >= startIndex && index < endIndex;
	}

	int getIndex() {
		return index;
	}

	void setNewIndex(int index) {
		this.index = index;
		targetPc = -1;
	}
}
