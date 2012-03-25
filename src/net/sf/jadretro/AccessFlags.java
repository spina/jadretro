/*
 * @(#) src/net/sf/jadretro/AccessFlags.java --
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

final class AccessFlags extends ClassEntity {

	private static final int ACC_FINAL = 0x10;

	private static final int ACC_STATIC = 0x8;

	private static final int ACC_SYNTHETIC = 0x1000;

	private static final int ACC_TRANSIENT = 0x80;

	private static final int ACC_VOLATILE = 0x40;

	private int flags;

	private AccessFlags(int flags) {
		this.flags = flags;
	}

	AccessFlags(InputStream in) throws IOException {
		flags = readUnsignedShort(in);
	}

	static AccessFlags makeStatic() {
		return new AccessFlags(ACC_STATIC);
	}

	void writeTo(OutputStream out) throws IOException {
		writeShort(out, flags);
	}

	boolean isFinal() {
		return (flags & ACC_FINAL) != 0;
	}

	boolean isStatic() {
		return (flags & ACC_STATIC) != 0;
	}

	boolean isSynthetic() {
		return (flags & ACC_SYNTHETIC) != 0;
	}

	boolean clearTransient() {
		if ((flags & ACC_TRANSIENT) == 0)
			return false;
		flags &= ~ACC_TRANSIENT;
		return true;
	}

	boolean clearVolatile() {
		if ((flags & ACC_VOLATILE) == 0)
			return false;
		flags &= ~ACC_VOLATILE;
		return true;
	}
}
