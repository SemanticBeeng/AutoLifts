package autolift.scalaz

import scalaz.{Functor, Foldable}
import autolift.{LiftExists, LiftedExists}
import export._

trait ScalazLiftExists[Obj, Fn] extends LiftExists[Obj, Fn]

@exports(Subclass)
object ScalazLiftExists extends LowPriorityScalazLiftExists {
	def apply[Obj, Fn](implicit lift: ScalazLiftExists[Obj, Fn]): Aux[Obj, Fn, lift.Out] = lift

	@export(Subclass)
	implicit def base[F[_], A, C >: A](implicit fold: Foldable[F]): Aux[F[A], C => Boolean, Boolean] =
		new ScalazLiftExists[F[A], C => Boolean]{
			type Out = Boolean

			def apply(fa: F[A], f: C => Boolean) = fold.all(fa)(f)
		}
}

trait LowPriorityScalazLiftExists{
	type Aux[Obj, Fn, Out0] = ScalazLiftExists[Obj, Fn]{ type Out = Out0 }

	@export(Subclass)
	implicit def recur[F[_], G, Fn](implicit functor: Functor[F], lift: LiftExists[G, Fn]): Aux[F[G], Fn, F[lift.Out]] =
		new ScalazLiftExists[F[G], Fn]{
			type Out = F[lift.Out]

			def apply(fg: F[G], f: Fn) = functor.map(fg){ g: G => lift(g, f) }
		}
}

trait LiftAnySyntax{
	implicit class LiftAnyOps[F[_], A](fa: F[A]){
		def liftAny[B](f: B => Boolean)(implicit lift: LiftExists[F[A], B => Boolean]): lift.Out = lift(fa, f)
	}
}

trait LiftAnyContext{
	def liftAny[A](f: A => Boolean): LiftedAny[A] = new LiftedExists(f)

	type LiftedAny[A] = LiftedExists[A]
}