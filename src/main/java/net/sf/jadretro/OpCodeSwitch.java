/*
 * @(#) net/sf/jadretro/OpCodeSwitch.java --
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
import java.util.Vector;

final class OpCodeSwitch extends OpByteCode {

	private static final int LOOKUPSWITCH = 0xab;

	private static final int TABLESWITCH = 0xaa;

	private/* final */CodeAbsLabel defaultLabel;

	private/* final */int lowValue;

	private/* final */int[] matchValues;

	private/* final */Vector gotoLabels;

	private OpCodeSwitch(CodeAbsLabel defaultLabel, int lowValue,
			int[] matchValues, Vector gotoLabels) {
		this.defaultLabel = defaultLabel;
		this.lowValue = lowValue;
		this.matchValues = matchValues;
		this.gotoLabels = gotoLabels;
	}

	private static int padSize(int curPc) {
		return ~curPc & 0x3;
	}

	static OpCodeSwitch decode(int op, InputStream in, int curPc)
			throws IOException {
		if (op != LOOKUPSWITCH && op != TABLESWITCH)
			return null;
		for (int skip = padSize(curPc); skip > 0; skip--) {
			if (in.read() != 0)
				throw new BadClassFileException();
		}
		CodeAbsLabel defaultLabel = new CodeAbsLabel(in, true, curPc);
		int lowValue;
		int count;
		int[] matchValues;
		if (op == LOOKUPSWITCH) {
			count = readInt(in);
			if (count < 0)
				throw new BadClassFileException();
			lowValue = 0;
			matchValues = new int[count];
		} else {
			lowValue = readInt(in);
			count = readInt(in) - lowValue + 1;
			if (count < 0)
				throw new BadClassFileException();
			matchValues = null;
		}
		Vector gotoLabels = new Vector(count);
		for (int i = 0; i < count; i++) {
			if (matchValues != null) {
				int value = readInt(in);
				if (i > 0 && matchValues[i - 1] >= value)
					throw new BadClassFileException();
				matchValues[i] = value;
			}
			gotoLabels.addElement(new CodeAbsLabel(in, true, curPc));
		}
		return new OpCodeSwitch(defaultLabel, lowValue, matchValues, gotoLabels);
	}

	void mapLabelsPc(int[] indices) throws BadClassFileException {
		defaultLabel.mapLabelsPc(indices);
		mapLabelsPcForArray(gotoLabels, indices);
	}

	void incLabelIndices(int startIndex, int incValue) {
		defaultLabel.incLabelIndices(startIndex, incValue);
		incLabelIndicesForArray(gotoLabels, startIndex, incValue);
	}

	void rebuildLabelsPc(int[] offsets) {
		defaultLabel.rebuildLabelsPc(offsets);
		rebuildLabelsPcForArray(gotoLabels, offsets);
	}

	int getLength(int curPc) {
		return padSize(curPc)
				+ ((matchValues != null ? matchValues.length << 1 : gotoLabels
						.size() + 1) << 2) + 9;
	}

	void writeRelTo(OutputStream out, int curPc) throws IOException {
		out.write(matchValues != null ? LOOKUPSWITCH : TABLESWITCH);
		for (int skip = padSize(curPc); skip > 0; skip--) {
			out.write(0);
		}
		defaultLabel.writeRelTo(out, true, curPc);
		int count = gotoLabels.size();
		if (matchValues != null) {
			writeInt(out, count);
		} else {
			writeInt(out, lowValue);
			writeInt(out, lowValue + count - 1);
		}
		for (int i = 0; i < count; i++) {
			if (matchValues != null) {
				writeInt(out, matchValues[i]);
			}
			((CodeAbsLabel) gotoLabels.elementAt(i)).writeRelTo(out, true,
					curPc);
		}
	}

	boolean isEqualTo(OpByteCode other, int startIndex, int deltaIndex,
			int endIndex2, int[] indexRef, int[] deltaVarRef, int argSlots,
			Hashtable diffVarsSet) {
		if (!(other instanceof OpCodeSwitch))
			return false;
		OpCodeSwitch opCodeSwitch = (OpCodeSwitch) other;
		int count = gotoLabels.size();
		if (opCodeSwitch.gotoLabels.size() != count
				|| lowValue != opCodeSwitch.lowValue
				|| !defaultLabel.isEqualTo(opCodeSwitch.defaultLabel,
						startIndex, deltaIndex, endIndex2, indexRef))
			return false;
		for (int i = 0; i < count; i++) {
			if (!((CodeAbsLabel) gotoLabels.elementAt(i)).isEqualTo(
					(CodeAbsLabel) opCodeSwitch.gotoLabels.elementAt(i),
					startIndex, deltaIndex, endIndex2, indexRef))
				return false;
		}
		if (matchValues != null) {
			if (opCodeSwitch.matchValues == null)
				return false;
			for (int i = 0; i < count; i++) {
				if (matchValues[i] != opCodeSwitch.matchValues[i])
					return false;
			}
		} else if (opCodeSwitch.matchValues != null)
			return false;
		return true;
	}

	boolean isTargetInRange(int startIndex, int endIndex) {
		if (defaultLabel.isInRange(startIndex, endIndex))
			return true;
		int count = gotoLabels.size();
		for (int i = 0; i < count; i++) {
			if (((CodeAbsLabel) gotoLabels.elementAt(i)).isInRange(startIndex,
					endIndex))
				return true;
		}
		return false;
	}

	boolean isUncondBranch() {
		return true;
	}
}
