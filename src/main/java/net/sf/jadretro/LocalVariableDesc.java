/*
 * @(#) net/sf/jadretro/LocalVariableDesc.java --
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

final class LocalVariableDesc extends ClassLabeledEntity {

	private/* final */CodeAbsLabel start;

	private/* final */CodeAbsLabel end;

	private/* final */ConstantRef name;

	private ConstantRef descriptor;

	private/* final */int index;

	LocalVariableDesc(InputStream in, ClassFile classFile) throws IOException {
		start = new CodeAbsLabel(in, null);
		end = new CodeAbsLabel(in, start);
		name = new ConstantRef(in, classFile, false);
		descriptor = new ConstantRef(in, classFile, false);
		index = readUnsignedShort(in);
	}

	void mapLabelsPc(int[] indices) throws BadClassFileException {
		start.mapLabelsPc(indices, false);
		end.mapLabelsPc(indices, true);
	}

	boolean removeLabelsInRange(int startIndex, int endIndex) {
		if (start.isInRange(startIndex, endIndex)) {
			if (end.isInRange(startIndex, endIndex + 1))
				return true;
			start.setNewIndex(endIndex);
		} else if (end.isInRange(startIndex, endIndex)) {
			end.setNewIndex(endIndex);
		}
		return false;
	}

	void incLabelIndices(int startIndex, int incValue) {
		start.incLabelIndices(startIndex, incValue);
		end.incLabelIndices(startIndex, incValue);
	}

	void rebuildLabelsPc(int[] offsets) {
		start.rebuildLabelsPc(offsets);
		end.rebuildLabelsPc(offsets);
	}

	void writeTo(OutputStream out) throws IOException {
		start.writeTo(out);
		end.writeRelTo(out, start);
		name.writeTo(out);
		descriptor.writeTo(out);
		writeShort(out, index);
	}

	ConstantRef descriptor() {
		return descriptor;
	}

	void changeDescriptor(ConstantRef descriptor) {
		this.descriptor = descriptor;
	}
}
