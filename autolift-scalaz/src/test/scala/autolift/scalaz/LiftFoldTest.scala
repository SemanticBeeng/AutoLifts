package autolift.test.scalaz

import scalaz._
import Scalaz._
import autolift.Scalaz._

class LiftFoldTest extends BaseSpec{
	"liftFold on a List" should "work" in{
		val in = List(1, 2, 3)
		val out = in.liftFold

		same[Int](out, 6)
	}

	"liftFold on a List[List]" should "work" in{
		val in = List(Nil, Nil, List(1))
		val out = in.liftFold

		same[List[Int]](out, List(1))
	}

	"liftFold on a State[List]" should "work" in{
		val in: State[Int,List[Int]] = State.state[Int,List[Int]](List(1, 2, 3))
		val out = in.liftFold

		same[Int](out.run(1)._2, 6)
	}
}