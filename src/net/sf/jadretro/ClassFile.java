/*
 * @(#) src/net/sf/jadretro/ClassFile.java --
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
import java.io.UTFDataFormatException;

import java.util.Vector;

final class ClassFile extends ClassEntity
{

 private static final int MAGIC_VALUE = 0xcafebabe;

 private static final int MAJOR_VER_BASE = 45;

 private static final int MINOR_VER_MIN = 3;

 private int minorVer;

 private int majorVer;

 private /* final */ Vector constants;

 private /* final */ AccessFlags accessFlags;

 private /* final */ ConstantRef thisClass;

 private /* final */ ConstantRef superClass;

 private /* final */ Vector interfaces;

 private /* final */ Vector fields;

 private /* final */ Vector methods;

 private /* final */ Vector attributes;

 ClassFile(InputStream in)
  throws IOException
 {
  if (readInt(in) != MAGIC_VALUE)
   throw new BadClassFileException();
  minorVer = readUnsignedShort(in);
  majorVer = readUnsignedShort(in);
  if (majorVer < MAJOR_VER_BASE || getJavaVer() > 0x40 ||
      (majorVer == MAJOR_VER_BASE && minorVer < MINOR_VER_MIN))
   throw new BadClassFileException();
  int constPoolSize = readUnsignedShort(in);
  if (constPoolSize == 0)
   throw new BadClassFileException();
  constants = new Vector(constPoolSize);
  constants.setSize(constPoolSize);
  constants.setElementAt(ConstantPoolEntry.EMPTY_ENTRY, 0);
  try
  {
   for (int i = 1; i < constPoolSize; i++)
   {
    ConstantPoolEntry entry = new ConstantPoolEntry(in, this);
    constants.setElementAt(entry, i);
    if (entry.isLongOrDouble())
    {
     if (++i == constPoolSize)
      throw new BadClassFileException();
     constants.setElementAt(ConstantPoolEntry.EMPTY_ENTRY, i);
    }
   }
  }
  catch (UTFDataFormatException e)
  {
   throw new BadClassFileException();
  }
  accessFlags = new AccessFlags(in);
  thisClass = new ConstantRef(in, this, false);
  superClass = new ConstantRef(in, this, true);
  int interfacesCount = readUnsignedShort(in);
  interfaces = new Vector(interfacesCount);
  while (interfacesCount-- > 0)
   interfaces.addElement(new ConstantRef(in, this, false));
  fields = readFieldsOrMethods(in, this);
  methods = readFieldsOrMethods(in, this);
  attributes = readAttributes(in, this);
  if (in.read() >= 0)
   throw new BadClassFileException();
 }

 private static Vector readFieldsOrMethods(InputStream in,
   ClassFile classFile)
  throws IOException
 {
  int count = readUnsignedShort(in);
  Vector entries = new Vector(count);
  while (count-- > 0)
   entries.addElement(new FieldMethodEntry(in, classFile));
  return entries;
 }

 void writeTo(OutputStream out)
  throws IOException
 {
  writeInt(out, MAGIC_VALUE);
  writeShort(out, minorVer);
  writeShort(out, majorVer);
  int constPoolSize = constants.size();
  writeCheckedUShort(out, constPoolSize);
  try
  {
   for (int i = 1; i < constPoolSize; i++)
   {
    ConstantPoolEntry entry = (ConstantPoolEntry) constants.elementAt(i);
    entry.writeTo(out);
    if (entry.isLongOrDouble())
     i++;
   }
  }
  catch (UTFDataFormatException e)
  {
   throw new ClassOverflowException();
  }
  accessFlags.writeTo(out);
  thisClass.writeTo(out);
  superClass.writeTo(out);
  writeToForArray(interfaces, out);
  writeToForArray(fields, out);
  writeToForArray(methods, out);
  writeToForArray(attributes, out);
 }

 int getJavaVer()
 {
  return (minorVer > 0 && majorVer > MAJOR_VER_BASE ? 2 : 1) +
          majorVer - MAJOR_VER_BASE;
 }

 void setJavaVer(int version)
 {
  if (version > 1)
  {
   majorVer = version + MAJOR_VER_BASE - 1;
   minorVer = 0;
  }
 }

 String className()
  throws BadClassFileException
 {
  return thisClass.classOrName().utfValue();
 }

 String getSuperClassName()
  throws BadClassFileException
 {
  return superClass.isZero() ? null : superClass.classOrName().utfValue();
 }

 int getMethodsCount()
 {
  return methods.size();
 }

 FieldMethodEntry getMethodAt(int i)
 {
  return (FieldMethodEntry) methods.elementAt(i);
 }

 void removeMethodAt(int i)
 {
  methods.removeElementAt(i);
 }

 int getFieldsCount()
 {
  return fields.size();
 }

 FieldMethodEntry getFieldAt(int i)
 {
  return (FieldMethodEntry) fields.elementAt(i);
 }

 int findField(String nameValue, String descriptorValue, boolean isStatic)
  throws BadClassFileException
 {
  int count = fields.size();
  for (int i = 0; i < count; i++)
  {
   FieldMethodEntry field = (FieldMethodEntry) fields.elementAt(i);
   if (field.accessFlags().isStatic() == isStatic &&
       nameValue.equals(field.name().utfValue()) &&
       descriptorValue.equals(field.descriptor().utfValue()))
    return i;
  }
  return -1;
 }

 void removeFieldAt(int i)
 {
  fields.removeElementAt(i);
 }

 void removeStaticAnonInnerClassInfo(String innerClassName)
  throws BadClassFileException
 {
  int count = attributes.size();
  for (int i = 0; i < count; i++)
  {
   AttrContent content = ((AttributeEntry) attributes.elementAt(i)).content();
   if (content instanceof AttrInnerClassContent)
   {
    if (((AttrInnerClassContent) content).removeStaticAnonInnerClass(
        innerClassName, className()))
     attributes.removeElementAt(i);
    break;
   }
  }
 }

 int getConstantPoolCount()
 {
  return constants.size();
 }

 ConstantPoolEntry getConstantAt(int i)
 {
  return (ConstantPoolEntry) constants.elementAt(i);
 }

 void changeClassConstAt(int i, String value)
 {
  constants.setElementAt(
   ConstantPoolEntry.makeClassString(addUtfConst(value), true), i);
 }

 void changeNameAndTypeConstAt(int i, String descriptorValue)
  throws BadClassFileException
 {
  constants.setElementAt(ConstantPoolEntry.makeNameAndType(
   ((ConstantPoolEntry) constants.elementAt(i)).content().classOrName(),
   addUtfConst(descriptorValue)), i);
 }

 private ConstantRef addConstant(ConstantPoolEntry entry)
 {
  for (int index = constants.size() - 1; index > 0; index--)
   if (entry.isEqualTo((ConstantPoolEntry) constants.elementAt(index)))
    return new ConstantRef(index, this);
  constants.addElement(entry);
  return new ConstantRef(constants.size() - 1, this);
 }

 ConstantRef addUtfConst(String value)
 {
  return addConstant(ConstantPoolEntry.makeUtf(value));
 }

 ConstantRef addClassStringConst(String value, boolean isClass)
 {
  return addConstant(ConstantPoolEntry.makeClassString(addUtfConst(value),
          isClass));
 }

 private ConstantRef addFieldNormMethodConst(ConstantRef classConst,
   ConstantRef name, ConstantRef descriptor, boolean isField)
 {
  return addConstant(ConstantPoolEntry.makeFieldNormMethod(classConst,
          addConstant(ConstantPoolEntry.makeNameAndType(name, descriptor)),
          isField));
 }

 ConstantRef addNormMethodConst(ConstantRef classConst, String nameValue,
   String descriptorValue)
 {
  ConstantRef name = addUtfConst(nameValue);
  return addFieldNormMethodConst(classConst, name,
          addUtfConst(descriptorValue), false);
 }

 ConstantRef addNormMethodConstFor(int methodIndex)
 {
  FieldMethodEntry method = getMethodAt(methodIndex);
  return addFieldNormMethodConst(thisClass, method.name(),
          method.descriptor(), false);
 }

 AttributeEntry makeSyntheticAttribute()
 {
  return new AttributeEntry(addUtfConst(AttrRawContent.syntheticName()),
          new AttrRawContent());
 }

 ConstantRef addStaticField(String nameValue, String typeValue)
  throws BadClassFileException
 {
  ConstantRef name = null;
  ConstantRef descriptor = null;
  int i = fields.size();
  while (i-- > 0)
  {
   FieldMethodEntry field = (FieldMethodEntry) fields.elementAt(i);
   name = field.name();
   descriptor = field.descriptor();
   if (nameValue.equals(name.utfValue()))
   {
    if (field.accessFlags().isStatic() &&
        typeValue.equals(descriptor.utfValue()))
     break;
    return null;
   }
  }
  if (i < 0)
  {
   name = addUtfConst(nameValue);
   descriptor = addUtfConst(typeValue);
   FieldMethodEntry field =
    new FieldMethodEntry(AccessFlags.makeStatic(), name, descriptor);
   field.addAttribute(makeSyntheticAttribute());
   fields.addElement(field);
  }
  return addFieldNormMethodConst(thisClass, name, descriptor, true);
 }

 int addStaticMethodNoExc(String nameValue, String descriptorValue)
  throws BadClassFileException
 {
  String paramDescr =
   descriptorValue.substring(0, descriptorValue.indexOf(')') + 1);
  for (int methodIndex = getMethodsCount() - 1;
       methodIndex >= 0; methodIndex--)
  {
   FieldMethodEntry method = getMethodAt(methodIndex);
   if (nameValue.equals(method.name().utfValue()))
   {
    String methodDescrValue = method.descriptor().utfValue();
    if (methodDescrValue.startsWith(paramDescr))
     return method.accessFlags().isStatic() &&
             !method.hasExceptionsSynthetic(true) &&
             descriptorValue.equals(methodDescrValue) ? methodIndex : -1;
   }
  }
  FieldMethodEntry method =
   new FieldMethodEntry(AccessFlags.makeStatic(), addUtfConst(nameValue),
   addUtfConst(descriptorValue));
  method.addAttribute(new AttributeEntry(
   addUtfConst(AttrCodeContent.nameValue()), new AttrCodeContent()));
  method.addAttribute(makeSyntheticAttribute());
  methods.addElement(method);
  return getMethodsCount() - 1;
 }
}
