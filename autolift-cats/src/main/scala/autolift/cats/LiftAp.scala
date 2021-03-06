package autolift.cats

import cats.{Functor, Apply, Unapply}
import autolift.{LiftAp, LiftApSyntax}

trait CatsLiftAp[Obj, Fn] extends LiftAp[Obj, Fn]

object CatsLiftAp extends LowPriorityCatsLiftAp {
  def apply[Obj, Fn](implicit lift: CatsLiftAp[Obj, Fn]): Aux[Obj, Fn, lift.Out] = lift

  implicit def base[F[_], A, B](implicit ap: Apply[F]): Aux[F[A], F[A => B], F[B]] =
    new CatsLiftAp[F[A], F[A => B]]{
      type Out = F[B]

      def apply(fa: F[A], f: F[A => B]) = ap.ap(f)(fa)
    }
}

trait LowPriorityCatsLiftAp extends LowPriorityCatsLiftAp1{
  implicit def recur[F[_], G, Fn](implicit functor: Functor[F], lift: LiftAp[G, Fn]): Aux[F[G], Fn, F[lift.Out]] =
    new CatsLiftAp[F[G], Fn]{
      type Out = F[lift.Out]

      def apply(fg: F[G], f: Fn) = functor.map(fg){ g: G => lift(g, f) }
    }
}

trait LowPriorityCatsLiftAp1{
  type Aux[Obj, Fn, Out0] = CatsLiftAp[Obj, Fn]{ type Out = Out0 }

  implicit def unrecur[FG, G, Fn](implicit unapply: Un.Apply[Functor, FG, G], lift: LiftAp[G, Fn]): Aux[FG, Fn, unapply.M[lift.Out]] =
    new CatsLiftAp[FG, Fn]{
      type Out = unapply.M[lift.Out]

      def apply(fg: FG, f: Fn) = unapply.TC.map(unapply.subst(fg)){ g: G => lift(g, f) }
    }
}

trait CatsLiftApSyntax extends LiftApSyntax with LowPriorityLiftApSyntax

trait LowPriorityLiftApSyntax{

  /// Syntax extension providing for a `liftAp` method.
  implicit class LowLiftApOps[FA](fa: FA)(implicit ev: Unapply[Functor, FA]){

    /**
     * Automatic Applicative lifting of the contained function `f` such that the application point is dictated by the
     * type of the Applicative.
     *
     * @param f the wrapped function to be lifted.
     * @tparam B the argument type of the function.
     * @tparam C the return type of the function.
     * @tparam M the higher-kinded type with an Applicative.
     */
    def liftAp[B, C, M[_]](f: M[B => C])(implicit lift: LiftAp[FA, M[B => C]]): lift.Out = lift(fa, f)
  }
}

final class LiftedAp[A, B, F[_]](protected val f: F[A => B])(implicit ap: Apply[F]){
  def andThen[C >: B, D](lf: LiftedAp[C, D, F]) = new LiftedAp(ap.ap(
    ap.map(lf.f){
      y: (C => D) => { x: (A => B) => x andThen y }
    }
  )(f))

  def compose[C, D <: A](lf: LiftedAp[C, D, F]) = lf andThen this

  def map[C](g: B => C): LiftedAp[A, C, F] = new LiftedAp(ap.map(f){ _ andThen g })

  def apply[That](that: That)(implicit lift: LiftAp[That, F[A => B]]): lift.Out = lift(that, f)
}

trait LiftedApImplicits{
  implicit def liftedApFunctor[A, F[_]] = new Functor[LiftedAp[A, ?, F]]{
    def map[B, C](lap: LiftedAp[A, B, F])(f: B => C) = lap map f
  }
}

trait LiftApContext{
  def liftAp[A, B, F[_]](f: F[A => B])(implicit ap: Apply[F]) = new LiftedAp(f)
}

