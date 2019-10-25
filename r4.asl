// mars robot 1

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r4,X,Y).

/*
@lg[atomic]
+garbage(r4) : not .desire(carry_from(r1))
   <- !carry_from(r1).
*/
+garb_found[source(r1)]
   <- .print("I received a found alert from ",r1);
      !carry_from(r1);
      .send(r1,tell,garb_delivered).

+!carry_from(R) : pos(r4,X,Y) 
   <- 
      -pos(last,X,Y);
      +pos(last,X,Y);
      !take(garb,R);
      !deliver(garb,last).      
+!carry_from(R) <- !carry_from(R).

+!deliver(S,L) : true
   <- !at(L);
      drop(S).

+!take(S,L) : true
   <- !at(L);
      !ensure_pick(S).

+!ensure_pick(S) : garbage(r4)
   <- pick(garb);
      !ensure_pick(S).
+!ensure_pick(_).

+!at(L) : at(L).
+!at(L) : pos(L,X,Y) 
   <- move_towards(X,Y);
      !at(L).
+!at(L) <- !at(L).
         
