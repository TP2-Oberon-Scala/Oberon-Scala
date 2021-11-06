package br.unb.cic.oberon.environment

import br.unb.cic.oberon.ast.{ArrayType, Expression, Procedure, Undef, UserDefinedType, Location}

import scala.collection.mutable.{ListBuffer, Map, Stack}

/**
 * The environment represents a memory region, which
 * could be used for interpreting oberon programs
 * or for type checking oberon programs.
 *
 * We have two kinds of memory: global and stack.
 * All global variables reside in the global area;
 * while local variables reside in the stack.
 *
 * Whenever we call a procedure, we push a memory
 * area (a hash map) into the stack. Whenever we
 * return from a procedure, we pop the stack.
 */
class Environment[T] {
  private var top_loc = 0
  private val locations = Map.empty[Location, T]
  private val global = Map.empty[String, Location]
  private val stack = Stack.empty[Map[String, Location]]
  private val procedures = Map.empty[String, Procedure]
  private val userDefinedTypes = Map.empty[String, UserDefinedType]

  private val userArrayTypes = Map.empty[String, ListBuffer[Expression]]

  def setGlobalVariable(name: String, value: T): Unit = {
    top_loc += 1
    global += name -> Location(top_loc)
    locations += Location(top_loc) -> value
  }

  def addUserDefinedType(userType: UserDefinedType) : Unit = {
    userDefinedTypes += userDefinedTypeName(userType) -> userType
    userType.baseType match {
      case ArrayType(length, variableType) => userArrayTypes += userType.name -> ListBuffer.fill(length)(Undef())
      case _ => ???
    }
  }

  def setLocalVariable(name: String, value: T) : Unit = {
    top_loc += 1
    if(stack.isEmpty) {
      stack.push(Map.empty[String, Location])
    }
    stack.top += name -> Location(top_loc)
    locations += Location(top_loc) -> value
  }

  def setVariable(name: String, value: T) : Unit = {
    if(stack.nonEmpty && stack.top.contains(name)) {
      locations(stack.top(name)) = value
    }
    else if(global.contains(name)) {
      locations(global(name)) = value
    }
    else throw new RuntimeException("Variable " + name + " is not defined")
  }

  def lookup(name: String) : Option[T] = {
    if(stack.nonEmpty && stack.top.contains(name)) Some(locations(stack.top(name)))
    else if(global.contains(name)) Some(locations(global(name)))
    else None
  }

  def reassignArray(name: String, index: Int, value: Expression) : Unit = {
    if (userArrayTypes.contains(name)) {
      userArrayTypes(name).update(index, value)
    }
  }

  def lookupArrayIndex(name: String, index: Int) : Option[Expression] = {
    if (userArrayTypes.contains(name) && userArrayTypes(name).length > index) {
      Some(userArrayTypes(name)(index))
    }
    else None
  }

  def lookupUserDefinedType(name: String) : Option[UserDefinedType] = userDefinedTypes.get(name)

  def declareProcedure(procedure: Procedure): Unit = procedures(procedure.name) = procedure

  def findProcedure(name: String): Procedure = procedures(name)

  def push(): Unit = stack.push(Map.empty[String, Location])

  def pop(): Unit = stack.pop()

  def userDefinedTypeName(userDefinedType: UserDefinedType) : String =
    userDefinedType.name
}
