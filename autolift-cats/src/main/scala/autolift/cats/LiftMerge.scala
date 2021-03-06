package autolift.cats

import autolift.{LiftMerge, LiftMergeSyntax}
import cats.{Functor, Apply, Unapply}

trait CatsLiftMerge[Obj1, Obj2] extends LiftMerge[Obj1, Obj2]

object CatsLiftMerge extends LowPriorityCatsLiftMerge{
  def apply[Obj1, Obj2](implicit lift: CatsLiftMerge[Obj1, Obj2]): Aux[Obj1, Obj2, lift.Out] = lift

  implicit def base[F[_], G, H](implicit ap: Apply[F]): Aux[F[G], F[H], F[(G, H)]] =
    new CatsLiftMerge[F[G], F[H]]{
      type Out = F[(G, H)]

      def apply(fg: F[G], fh: F[H]) = ap.ap{
        ap.map(fh){ h: H => (_: G, h) }
      }(fg)
    }
}

trait LowPriorityCatsLiftMerge extends LowPriorityCatsLiftMerge1{
  implicit def recur[F[_], G, H](implicit lift: LiftMerge[G, H], functor: Functor[F]): Aux[F[G], H, F[lift.Out]] =
    new CatsLiftMerge[F[G], H]{
      type Out = F[lift.Out]

      def apply(fg: F[G], h: H) = functor.map(fg){ g: G => lift(g, h) }
    }
}

trait LowPriorityCatsLiftMerge1{
  type Aux[Obj1, Obj2, Out0] = CatsLiftMerge[Obj1, Obj2]{ type Out = Out0 }

  implicit def unrecur[FG, G, H](implicit unapply: Un.Apply[Functor, FG, G], 
                                          lift: LiftMerge[G, H]): Aux[FG, H, unapply.M[lift.Out]] =
    new CatsLiftMerge[FG, H]{
      type Out = unapply.M[lift.Out]

      def apply(fg: FG, h: H) = unapply.TC.map(unapply.subst(fg)){ g: G => lift(g, h) }
    }
}

trait CatsLiftMergeSyntax extends LiftMergeSyntax with LowPriorityLiftMergeSyntax

trait LowPriorityLiftMergeSyntax{

  /// Syntax extension providing for a `liftMerge` method.
  implicit class LowLiftMergeOps[FA](fa: FA)(implicit ev: Unapply[Functor, FA]){

    /**
     * Automatic lifting of a `merge` operation, type merged dependent on the nested type structure.
     *
     * @param that the object to be merged.
     * @tparam That the argument type of the object to be merged.
     */
    def liftMerge[That](that: That)(implicit lift: LiftMerge[FA, That]): lift.Out = lift(fa, that)
  }
}