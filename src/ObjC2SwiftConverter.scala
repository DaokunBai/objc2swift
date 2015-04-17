/**
 * This file is part of objc2swift. 
 * https://github.com/yahoojapan/objc2swift
 * 
 * Copyright (c) 2015 Yahoo Japan Corporation
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import ObjCParser._
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.{ParseTree, ParseTreeProperty}
import collection.JavaConversions._

class ObjC2SwiftConverter extends ObjCBaseVisitor[String] {
  val properties = new ParseTreeProperty[String]()

  def concatChildResults(node: ParseTree, glue: String): String = {
    val children = for(i <- 0 until node.getChildCount) yield node.getChild(i)
    return concatResults(children.toList, glue)
  }

  def concatResults(nodes: List[ParseTree], glue: String): String = {
    val sb = new StringBuilder()
    for(node <- nodes) {
      if(sb.length > 0)
        sb.append(glue)

      val r = visit(node)
      if(r != null)
        sb.append(r)
    }
    return sb.toString
  }

  override def visitTranslation_unit(ctx: ObjCParser.Translation_unitContext): String = {
    return concatChildResults(ctx, "\n")
  }

  override def visitExternal_declaration(ctx: ObjCParser.External_declarationContext): String = {
    return concatChildResults(ctx, "\n")
  }

  override def visitClass_interface(ctx: ObjCParser.Class_interfaceContext): String = {
    val sb = new StringBuilder()
    sb.append("class " + ctx.class_name.getText())

    if(ctx.superclass_name() != null) {
      sb.append(" : ")
      sb.append(ctx.superclass_name().getText())
    }
    if(ctx.protocol_reference_list() != null) {
      val protocols = ctx.protocol_reference_list().protocol_list().children.filter(_.isInstanceOf[ObjCParser.Protocol_nameContext])
      sb.append(protocols.foldLeft("")(_ + ", " + _.getText))
    }

    sb.append(" {\n")
    if(ctx.interface_declaration_list() != null) {
      var variable_of_type = ""
      var property_attributes = ""
      val property_declaration_buffer: collection.mutable.Buffer[ObjCParser.Property_declarationContext] = ctx.interface_declaration_list().property_declaration()

      property_declaration_buffer.foreach {e =>
        if(e.property_attributes_declaration() != null) {
          e.property_attributes_declaration().property_attributes_list().property_attribute().foreach { k =>
            if (k.getText() == "weak" || k.getText == "strong") {
              property_attributes = k.getText()
            }
          }
        }

        if(e.struct_declaration() != null) {
          val specifier_qualifier_list = e.struct_declaration().specifier_qualifier_list()
          val struct_declarator_list = e.struct_declaration().struct_declarator_list()

          specifier_qualifier_list.type_specifier().foreach { i =>
            val class_name = i.class_name.getText()
            if (class_name == "IBOutlet") {
              sb.append("\t@" + class_name + " " + property_attributes + " ")
            } else {
              variable_of_type = class_name
            }
          }

          struct_declarator_list.struct_declarator.foreach { j =>
            val direct_declarator = j.declarator.direct_declarator()
            if (direct_declarator != null) {
              val identifier = direct_declarator.identifier().getText()
              sb.append("var " + identifier + ":" + variable_of_type +"!")
            }
          }
        }
      }

      sb.append("\n")
    }
    sb.append("}")

    return sb.toString()
  }
}
