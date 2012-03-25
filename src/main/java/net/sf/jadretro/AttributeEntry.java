/*
 * @(#) net/sf/jadretro/AttributeEntry.java --
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class AttributeEntry extends ClassLabeledEntity {

	private/* final */ConstantRef name;

	private/* final */AttrContent content;

	AttributeEntry(ConstantRef name, AttrContent content) {
		this.name = name;
		this.content = content;
	}

	AttributeEntry(InputStream in, ClassFile classFile) throws IOException {
		name = new ConstantRef(in, classFile, false);
		int len = readInt(in);
		if (len < 0)
			throw new BadClassFileException();
		byte[] bytes = new byte[len];
		readFully(in, bytes);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			content = decodeContent(bais, len, name, classFile);
		} catch (EOFException e) {
			throw new BadClassFileException();
		}
		if (bais.read() >= 0)
			throw new BadClassFileException();
	}

	private static AttrContent decodeContent(InputStream in, int len,
			ConstantRef name, ClassFile classFile) throws IOException {
		String nameValue = name.utfValue();
		if (AttrCodeContent.nameValue().equals(nameValue))
			return new AttrCodeContent(in, classFile);
		if (AttrLineNumsContent.nameValue().equals(nameValue))
			return new AttrLineNumsContent(in);
		if (AttrLocalVarsContent.nameValue().equals(nameValue)
				|| "LocalVariableTypeTable".equals(nameValue))
			return new AttrLocalVarsContent(in, classFile);
		if (AttrInnerClassContent.nameValue().equals(nameValue))
			return new AttrInnerClassContent(in, classFile);
		return new AttrRawContent(in, len);
	}

	void mapLabelsPc(int[] indices) throws BadClassFileException {
		content.mapLabelsPc(indices);
	}

	boolean removeLabelsInRange(int startIndex, int endIndex) {
		return content.removeLabelsInRange(startIndex, endIndex);
	}

	void incLabelIndices(int startIndex, int incValue) {
		content.incLabelIndices(startIndex, incValue);
	}

	void rebuildLabelsPc(int[] offsets) {
		content.rebuildLabelsPc(offsets);
	}

	void writeTo(OutputStream out) throws IOException {
		name.writeTo(out);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		content.writeTo(baos);
		writeInt(out, baos.size());
		baos.writeTo(out);
	}

	String getNameValue() throws BadClassFileException {
		return name.utfValue();
	}

	AttrContent content() {
		return content;
	}
}
