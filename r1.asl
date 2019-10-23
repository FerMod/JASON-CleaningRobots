// mars robot 1

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r1,X,Y).

/* Initial goal */

!check(slots).

/* Plans */

+!check(slots) : not garbage(r1)
   <- next(slot);
      !check(slots).
+!check(slots).


@lg[atomic]
+garbage(r1) : not .desire(carry_to(r2))
   <- !carry_to(r2).

+!carry_to(R): pos(r1,X,Y)
   <- // remember where to go back
      -+pos(last,X,Y);

      // carry garbage to r2
      !take(garb,R);

      // goes back and continue to check
      !at(last);
      !check(slots).
+!carry_to(R) <- !carry_to(R).

+!take(S,L) : true
   <- !ensure_pick(S);
      !at(L);
      drop(S).

+!ensure_pick(S) : garbage(r1)
   <- pick(garb);
      !ensure_pick(S).
+!ensure_pick(_).

+!at(L) : at(L).
+!at(L) : pos(L,X,Y) <- 
		   move_towards(X,Y);
           !at(L).
+!at(L) <- !at(L).
		   
