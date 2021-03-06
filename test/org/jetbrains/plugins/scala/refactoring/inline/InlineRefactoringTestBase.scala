package org.jetbrains.plugins.scala
package refactoring
package inline


import base.ScalaLightPlatformCodeInsightTestCaseAdapter
import com.intellij.psi.util.PsiTreeUtil
import lang.lexer.ScalaTokenTypes
import lang.psi.api.base.patterns.ScBindingPattern
import util.ScalaUtils
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}

import com.intellij.refactoring.inline.GenericInlineHandler
import lang.psi.api.ScalaFile
import java.io.File
import com.intellij.openapi.vfs.{CharsetToolkit, LocalFileSystem}
import lang.refactoring.inline.ScalaInlineHandler
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.io.FileUtil

/**
 * User: Alexander Podkhalyuzin
 * Date: 16.06.2009
 */

abstract class InlineRefactoringTestBase extends ScalaLightPlatformCodeInsightTestCaseAdapter {
  val caretMarker = "/*caret*/"

  protected def folderPath = baseRootPath() + "inline/"

  protected def doTest() {
    import _root_.junit.framework.Assert._
    val filePath = folderPath + getTestName(false) + ".scala"
    val file = LocalFileSystem.getInstance.findFileByPath(filePath.replace(File.separatorChar, '/'))
    assert(file != null, "file " + filePath + " not found")
    val fileText = StringUtil.convertLineSeparators(FileUtil.loadFile(new File(file.getCanonicalPath), CharsetToolkit.UTF8))
    configureFromFileTextAdapter(getTestName(false) + ".scala", fileText)
    val scalaFile = getFileAdapter.asInstanceOf[ScalaFile]
    val offset = fileText.indexOf(caretMarker) + caretMarker.length
    assert(offset != -1, "Not specified caret marker in test case. Use /*caret*/ in scala file for this.")
    val element = scalaFile.findElementAt(offset)
    val fileEditorManager = FileEditorManager.getInstance(getProjectAdapter)
    val editor = fileEditorManager.openTextEditor(new OpenFileDescriptor(getProjectAdapter, file, offset), false)

    var res: String = null

    val lastPsi = scalaFile.findElementAt(scalaFile.getText.length - 1)
    
    //start to inline
    try {
      ScalaUtils.runWriteActionDoNotRequestConfirmation(new Runnable {
        def run() {
          GenericInlineHandler.invoke(PsiTreeUtil.
                  getParentOfType(element, classOf[ScBindingPattern]), editor, new ScalaInlineHandler)
        }
      }, getProjectAdapter, "Test")
      res = scalaFile.getText.substring(0, lastPsi.getTextOffset).trim//getImportStatements.map(_.getText()).mkString("\n")
    }
    catch {
      case e: Exception => assert(assertion = false, message = e.getMessage + "\n" + e.getStackTrace)
    }

    val text = lastPsi.getText
    val output = lastPsi.getNode.getElementType match {
      case ScalaTokenTypes.tLINE_COMMENT => text.substring(2).trim
      case ScalaTokenTypes.tBLOCK_COMMENT | ScalaTokenTypes.tDOC_COMMENT =>
        text.substring(2, text.length - 2).trim
      case _ => {
        assertTrue("Test result must be in last comment statement.", false)
        ""
      }
    }
    assertEquals(output, res.trim)
  }
}