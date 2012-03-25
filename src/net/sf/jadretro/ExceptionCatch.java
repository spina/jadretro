/*
 * @(#) src/net/sf/jadretro/ExceptionCatch.java --
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

final class ExceptionCatch extends ClassLabeledEntity {

	private/* final */CodeAbsLabel start;

	private/* final */CodeAbsLabel end;

	private/* final */CodeAbsLabel handler;

	private/* final */ConstantRef catchType;

	ExceptionCatch(int startIndex, int endIndex, int handlerIndex,
			ConstantRef catchType) {
		(start = new CodeAbsLabel()).setNewIndex(startIndex);
		(end = new CodeAbsLabel()).setNewIndex(endIndex);
		(handler = new CodeAbsLabel()).setNewIndex(handlerIndex);
		this.catchType = catchType;
	}

	ExceptionCatch(InputStream in, ClassFile classFile) throws IOException {
		start = new CodeAbsLabel(in, null);
		end = new CodeAbsLabel(in, null);
		handler = new CodeAbsLabel(in, null);
		catchType = new ConstantRef(in, classFile, true);
	}

	void mapLabelsPc(int[] indices) throws BadClassFileException {
		start.mapLabelsPc(indices, false);
		end.mapLabelsPc(indices, true);
		if (start.getIndex() >= end.getIndex())
			throw new BadClassFileException();
		handler.mapLabelsPc(indices, false);
	}

	boolean removeLabelsInRange(int startIndex, int endIndex) {
		if (handler.isInRange(startIndex, endIndex))
			return true;
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
		handler.incLabelIndices(startIndex, incValue);
	}

	void rebuildLabelsPc(int[] offsets) {
		start.rebuildLabelsPc(offsets);
		end.rebuildLabelsPc(offsets);
		handler.rebuildLabelsPc(offsets);
	}

	void writeTo(OutputStream out) throws IOException {
		start.writeTo(out);
		end.writeTo(out);
		handler.writeTo(out);
		catchType.writeTo(out);
	}

	CodeAbsLabel start() {
		return start;
	}

	CodeAbsLabel end() {
		return end;
	}

	CodeAbsLabel handler() {
		return handler;
	}

	boolean isAnyType() {
		return catchType.isZero();
	}

	boolean isSameCatchType(ExceptionCatch other) {
		return catchType.isEqualTo(other.catchType);
	}
}
