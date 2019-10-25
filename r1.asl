// mars robot 1

/* Initial beliefs */

//at(P) :- pos(P,X,Y) & pos(r1,X,Y).

/* Initial goal */

!check(slots).

/* Plans */

+!check(slots) : not garbage(r1)
   <- next(slot);
      !check(slots).
+!check(slots).


@lg[atomic]
+garbage(r1) : true
   <- .send(r4,tell,garb_found).

+garb_delivered[source(r4)]
   <- .print("I received delivered confirmation from ",r4);
      !check(slots). 
