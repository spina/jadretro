/*
 * @(#) src/net/sf/jadretro/AttrCodeContent.java --
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

import java.util.Hashtable;
import java.util.Vector;

final class AttrCodeContent extends AttrContent
{

 private static final OpByteCode DUMMY_OPCODE = new OpCodeSimple(0);

 private int maxStack;

 private int maxLocals;

 private final Vector opByteCodes = new Vector();

 private /* final */ Vector exceptions;

 private /* final */ Vector attributes;

 AttrCodeContent()
 {
  exceptions = new Vector(1);
  attributes = new Vector(0);
 }

 AttrCodeContent(InputStream in, ClassFile classFile)
  throws IOException
 {
  maxStack = readUnsignedShort(in);
  maxLocals = readUnsignedShort(in);
  int[] indices = readCodeFrom(in, classFile);
  int exceptionsCount = readUnsignedShort(in);
  exceptions = new Vector(exceptionsCount);
  while (exceptionsCount-- > 0)
   exceptions.addElement(new ExceptionCatch(in, classFile));
  attributes = readAttributes(in, classFile);
  mapLabelsPcInner(indices);
 }

 static String nameValue()
 {
  return "Code";
 }

 private int[] readCodeFrom(InputStream in, ClassFile classFile)
  throws IOException
 {
  if (getOpCodesCount() != 0)
   throw new BadClassFileException();
  int codeLen = readInt(in);
  if (codeLen <= 0)
   throw new BadClassFileException();
  byte[] bytes = new byte[codeLen];
  readFully(in, bytes);
  ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
  int[] indices = new int[bytes.length + 1];
  for (int i = indices.length - 2; i > 0; i--)
   indices[i] = -1;
  try
  {
   int remain;
   while ((remain = bais.available()) > 0)
   {
    int op = readUnsignedByte(bais);
    OpByteCode opCode = OpCodeVar.decode(op, bais);
    int curPc = bytes.length - remain;
    indices[curPc] = opByteCodes.size();
    if (opCode == null)
    {
     opCode = OpCodeJump.decode(op, bais, curPc);
     if (opCode == null)
     {
      opCode = OpCodeSwitch.decode(op, bais, curPc);
      if (opCode == null)
      {
       opCode = OpCodeConst.decode(op, bais, classFile);
       if (opCode == null)
        opCode = new OpCodeSimple(op);
      }
     }
    }
    opByteCodes.addElement(opCode);
   }
  }
  catch (EOFException e)
  {
   throw new BadClassFileException();
  }
  indices[bytes.length] = opByteCodes.size();
  return indices;
 }

 private void mapLabelsPcInner(int[] indices)
  throws BadClassFileException
 {
  mapLabelsPcForArray(opByteCodes, indices);
  mapLabelsPcForArray(exceptions, indices);
  mapLabelsPcForArray(attributes, indices);
 }

 private void incLabelIndicesInner(int startIndex, int incValue)
 {
  incLabelIndicesForArray(opByteCodes, startIndex, incValue);
  incLabelIndicesForArray(exceptions, startIndex, incValue);
  incLabelIndicesForArray(attributes, startIndex, incValue);
 }

 private void rebuildLabelsPcInner(int[] offsets)
 {
  rebuildLabelsPcForArray(opByteCodes, offsets);
  rebuildLabelsPcForArray(exceptions, offsets);
  rebuildLabelsPcForArray(attributes, offsets);
 }

 private int[] evalCodeOffsets()
 {
  int count = opByteCodes.size();
  int[] offsets = new int[count + 1];
  int curPc = 0;
  for (int i = 0; i < count; i++)
  {
   offsets[i] = curPc;
   curPc += ((OpByteCode) opByteCodes.elementAt(i)).getLength(curPc);
  }
  offsets[count] = curPc;
  return offsets;
 }

 private void writeCodeTo(OutputStream out)
  throws IOException
 {
  int count = opByteCodes.size();
  ByteArrayOutputStream baos =
   new ByteArrayOutputStream((count << 1) + (count >> 2) + 16);
  for (int i = 0; i < count; i++)
  {
   int curPc = baos.size();
   ((OpByteCode) opByteCodes.elementAt(i)).writeRelTo(baos, curPc);
  }
  int codeLen = baos.size();
  if (((codeLen - 1) & ~0xffff) != 0)
   throw new ClassOverflowException();
  writeInt(out, codeLen);
  baos.writeTo(out);
 }

 void writeTo(OutputStream out)
  throws IOException
 {
  rebuildLabelsPcInner(evalCodeOffsets());
  writeCheckedUShort(out, maxStack);
  writeCheckedUShort(out, maxLocals);
  writeCodeTo(out);
  writeToForArray(exceptions, out);
  writeToForArray(attributes, out);
 }

 int maxStack()
 {
  return maxStack;
 }

 void setMaxStack(int maxStack)
 {
  this.maxStack = maxStack;
 }

 int maxLocals()
 {
  return maxLocals;
 }

 void setMaxLocals(int maxLocals)
 {
  this.maxLocals = maxLocals;
 }

 int getCatchesCount()
 {
  return exceptions.size();
 }

 ExceptionCatch getCatch(int i)
 {
  return (ExceptionCatch) exceptions.elementAt(i);
 }

 void addCatch(int startIndex, int endIndex, int handlerIndex,
   ConstantRef catchType)
 {
  exceptions.addElement(new ExceptionCatch(startIndex, endIndex, handlerIndex,
   catchType));
 }

 void removeCatch(int i)
 {
  exceptions.removeElementAt(i);
 }

 int getAttributesCount()
 {
  return attributes.size();
 }

 AttributeEntry getAttributeAt(int i)
 {
  return (AttributeEntry) attributes.elementAt(i);
 }

 int getOpCodesCount()
 {
  return opByteCodes.size();
 }

 private OpByteCode getOpCodeAt(int index)
 {
  return index >= 0 && opByteCodes.size() > index ?
          (OpByteCode) opByteCodes.elementAt(index) : DUMMY_OPCODE;
 }

 boolean isAthrowAt(int index)
 {
  return getOpCodeAt(index).isAthrow();
 }

 boolean isInvokeMethodAt(int index)
 {
  return getOpCodeAt(index).isInvokeMethod();
 }

 boolean isMonitorexitAt(int index)
 {
  return getOpCodeAt(index).isMonitorexit();
 }

 boolean isTargetInRangeAt(int index, int startIndex, int endIndex)
 {
  return getOpCodeAt(index).isTargetInRange(startIndex, endIndex);
 }

 boolean isRetAt(int index, int varInd)
 {
  OpByteCode opCode = getOpCodeAt(index);
  return opCode.isRetXLoad(true) && opCode.getVarIndex() == varInd;
 }

 boolean isXConstZeroAt(int index)
 {
  return getOpCodeAt(index).isXConstZero();
 }

 boolean isXLoadAt(int index)
 {
  return getOpCodeAt(index).isRetXLoad(false);
 }

 boolean isXReturnAt(int index)
 {
  return getOpCodeAt(index).isXReturn();
 }

 int getAstoreAloadVarAt(int index, boolean isStore)
 {
  OpByteCode opCode = getOpCodeAt(index);
  return opCode.isAstoreAload(isStore) ? opCode.getVarIndex() : -1;
 }

 int getJsrGotoTargetIndexAt(int index, boolean isJsr)
 {
  OpByteCode opCode = getOpCodeAt(index);
  return opCode.isJsrGoto(isJsr) ? opCode.getTargetLabel().getIndex() : -1;
 }

 ConstantRef getInvokevirtualRefAt(int index)
 {
  OpByteCode opCode = getOpCodeAt(index);
  return opCode.isInvokevirtual() ? opCode.getConstRef() : null;
 }

 ConstantRef getPutfieldRefAt(int index)
 {
  OpByteCode opCode = getOpCodeAt(index);
  return opCode.isPutfield() ? opCode.getConstRef() : null;
 }

 ConstantRef getPutGetstaticRefAt(int index, boolean isPut)
 {
  OpByteCode opCode = getOpCodeAt(index);
  return opCode.isPutGetstatic(isPut) ? opCode.getConstRef() : null;
 }

 ConstantRef getConstRefAt(int index)
 {
  return getOpCodeAt(index).getConstRef();
 }

 int findCheckcast(int startIndex)
 {
  while (opByteCodes.size() > startIndex)
  {
   if (((OpByteCode) opByteCodes.elementAt(startIndex)).isCheckcast())
    return startIndex;
   startIndex++;
  }
  return -1;
 }

 int findLdcClass(int startIndex, int endIndex)
 {
  while (startIndex < endIndex)
  {
   OpByteCode opCode = (OpByteCode) opByteCodes.elementAt(startIndex);
   if (opCode.isLdc() && opCode.getConstRef().isClassConst())
    return startIndex;
   startIndex++;
  }
  return -1;
 }

 int findInvokestaticSpecial(int startIndex, boolean isStatic)
 {
  while (opByteCodes.size() > startIndex)
  {
   if (((OpByteCode) opByteCodes.elementAt(startIndex)).isInvokestaticSpecial(
       isStatic))
    return startIndex;
   startIndex++;
  }
  return -1;
 }

 int findPutGetstatic(int startIndex)
 {
  while (opByteCodes.size() > startIndex)
  {
   OpByteCode opCode = (OpByteCode) opByteCodes.elementAt(startIndex);
   if (opCode.isPutGetstatic(true) || opCode.isPutGetstatic(false))
    return startIndex;
   startIndex++;
  }
  return -1;
 }

 int findRetAload(int startIndex, int varInd, boolean isRet)
 {
  while (opByteCodes.size() > startIndex)
  {
   OpByteCode opCode = (OpByteCode) opByteCodes.elementAt(startIndex);
   if ((isRet ? opCode.isRetXLoad(true) : opCode.isAstoreAload(false)) &&
       opCode.getVarIndex() == varInd)
    return startIndex;
   startIndex++;
  }
  return -1;
 }

 boolean isSameCodeRegions(int startIndex, int startIndex2, int endIndex2,
   int argSlots, int[] indexRef)
 {
  int deltaIndex = startIndex2 - startIndex;
  if (startIndex < 0 || deltaIndex < 0 || opByteCodes.size() < endIndex2)
   return false;
  int[] deltaVarRef = new int[1];
  Hashtable diffVarsSet = new Hashtable();
  for (int i = startIndex2; i < endIndex2; i++)
   if (!((OpByteCode) opByteCodes.elementAt(i - deltaIndex)).isEqualTo(
       (OpByteCode) opByteCodes.elementAt(i), startIndex, deltaIndex,
       endIndex2, indexRef, deltaVarRef, argSlots, diffVarsSet))
    return false;
  if (deltaVarRef[0] < 0)
   return false;
  if (indexRef[0] < 0)
   return true;
  int endIndex = endIndex2 - deltaIndex;
  if (indexRef[0] == endIndex ||
      getJsrGotoTargetIndexAt(endIndex, false) == indexRef[0])
  {
   indexRef[0] = -1;
   return true;
  }
  return (indexRef[0] < startIndex || indexRef[0] > endIndex) &&
          isNewBranchAt(endIndex);
 }

 private boolean isNewBranchAt(int index)
 {
  return index == 0 || getOpCodeAt(index - 1).isUncondBranch();
 }

 boolean hasBranchesOutsideIntoCode(int startIndex, int count)
 {
  int endIndex = startIndex + count;
  if (count > 0 && opByteCodes.size() >= endIndex)
  {
   int exceptionsCount = exceptions.size();
   for (int i = 0; i < exceptionsCount; i++)
   {
    ExceptionCatch excCatch = (ExceptionCatch) exceptions.elementAt(i);
    if (excCatch.handler().isInRange(startIndex, endIndex) &&
        (excCatch.start().getIndex() + 1 < startIndex ||
        excCatch.end().getIndex() > endIndex))
     return true;
   }
   int i = opByteCodes.size();
   do
   {
    if (i == endIndex)
     i = startIndex - 1;
    if (--i < 0)
     break;
    if (((OpByteCode) opByteCodes.elementAt(i)).isTargetInRange(startIndex,
        endIndex))
     return true;
   } while (true);
  }
  return false;
 }

 void removeCodeAt(int startIndex, int count)
 {
  if (count != 0)
  {
   int endIndex = startIndex + count;
   if (count > 0)
   {
    removeLabelsInRangeForArray(exceptions, startIndex, endIndex);
    removeLabelsInRangeForArray(attributes, startIndex != 0 ||
     opByteCodes.size() == endIndex ? startIndex : 1, endIndex);
    incLabelIndicesInner(endIndex, -count);
    do
    {
     opByteCodes.removeElementAt(--endIndex);
    } while (startIndex < endIndex);
   }
    else
    {
     incLabelIndicesInner(endIndex + 1, -count);
     insCodeNopsInner(endIndex, -count);
    }
  }
 }

 void insertCodeNopsAt(int startIndex, int count)
 {
  if (count > 0)
  {
   incLabelIndicesInner(startIndex, count);
   insCodeNopsInner(startIndex, count);
  }
 }

 private void insCodeNopsInner(int startIndex, int count)
 {
  do
  {
   opByteCodes.insertElementAt(new OpCodeSimple(0), startIndex);
   startIndex++;
  } while (--count > 0);
 }

 void moveCodeBlockAt(int startIndex, int count, int destIndex)
 {
  insertCodeNopsAt(destIndex, count);
  if (startIndex >= destIndex)
   startIndex += count;
  for (int i = 0; i < count; i++)
   opByteCodes.setElementAt(opByteCodes.elementAt(startIndex + i),
    destIndex + i);
  removeCodeAt(startIndex, count);
 }

 void putAreturnAt(int index)
 {
  opByteCodes.setElementAt(OpCodeSimple.makeAreturn(), index);
 }

 void putAthrowAt(int index)
 {
  opByteCodes.setElementAt(OpCodeSimple.makeAthrow(), index);
 }

 void putDupAt(int index)
 {
  opByteCodes.setElementAt(OpCodeSimple.makeDup(), index);
 }

 void putIconstZeroAt(int index)
 {
  opByteCodes.setElementAt(OpCodeSimple.makeIconstZero(), index);
 }

 void putMonitorexitAt(int index)
 {
  opByteCodes.setElementAt(OpCodeSimple.makeMonitorexit(), index);
 }

 void putIfnonnullAt(int index, int targetIndex)
 {
  opByteCodes.setElementAt(OpCodeJump.makeIfnonnull(targetIndex), index);
 }

 void putJsrGotoAt(int index, int targetIndex, boolean isJsr)
 {
  opByteCodes.setElementAt(OpCodeJump.makeJsrGoto(targetIndex, isJsr), index);
 }

 void putAstoreAloadAt(int index, int varInd, boolean isStore)
 {
  opByteCodes.setElementAt(OpCodeVar.makeAstoreAload(varInd, isStore), index);
 }

 void putRetAt(int index, int varInd)
 {
  opByteCodes.setElementAt(OpCodeVar.makeRet(varInd), index);
 }

 void putInvokestaticAt(int index, ConstantRef method)
 {
  opByteCodes.setElementAt(OpCodeConst.makeInvokestatic(method), index);
 }

 void putInvokevirtualSpecialAt(int index, ConstantRef classConst,
   String nameValue, String descriptorValue, boolean isVirtual,
   ClassFile classFile)
 {
  opByteCodes.setElementAt(OpCodeConst.makeInvokevirtualSpecial(
   classFile.addNormMethodConst(classConst, nameValue, descriptorValue),
   isVirtual), index);
 }

 void putLdcClassStringAt(int index, String value, boolean isClass,
   ClassFile classFile)
 {
  opByteCodes.setElementAt(OpCodeConst.makeLdc(
   classFile.addClassStringConst(value, isClass)), index);
 }

 void putNewCheckcastAt(int index, ConstantRef classConst, boolean isNew)
 {
  opByteCodes.setElementAt(OpCodeConst.makeNewCheckcast(classConst, isNew),
   index);
 }

 void putPutGetstaticAt(int index, ConstantRef field, boolean isPut)
 {
  opByteCodes.setElementAt(OpCodeConst.makePutGetstatic(field, isPut), index);
 }
}
