package spire

import spire.syntax._

import org.scalatest.FunSuite

import collection.mutable

class SyntaxTest extends FunSuite {
  test("simple cfor") {
    val l = mutable.ListBuffer[Int]()    
    cfor(0)(_ < 5, _ + 1) { x => 
      l.append(x)
    }

    assert(l.toSeq === Seq(0,1,2,3,4))
  }

  test("nested cfor") {
    val s = mutable.Set[Int]()
    cfor(0)(_ < 10, _ + 1) { x =>
      cfor(10)(_ < 100, _ + 10) { y =>
        s.add(x+y)
      }
    }
    assert(s.toSeq.sorted === (10 to 99))
  }

  test("symbol collision cfor") {
    val b = mutable.ArrayBuffer.empty[Int]
    cfor(0)(_ < 3, _ + 1) { x =>
      cfor(0)(_ < 2, _ + 1) { y => 
        val x = y
        b += x
      }
    }
    assert(b.toList === List(0, 1, 0, 1, 0, 1))
  }

  test("functions with side effects in cfor") {
    val b = mutable.ArrayBuffer.empty[Int]
    var v = 0
    cfor(0)({ v += 1; _ < 3 }, { v += 10; _ + 1})({
      v += 100
      x => {
        b += x
      }
    })
    assert(v == 111, s"v == $v")
    assert(b.toList === List(0, 1, 2), s"b.toList == ${b.toList}")
  }
}