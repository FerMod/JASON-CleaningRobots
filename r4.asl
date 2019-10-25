// mars robot 4

/* Initial beliefs */

at4(P) :- pos(P,X,Y) & pos(r4,X,Y).

/* Initial goal */

!check4(slots).

/* Plans */

+!check4(slots) : not garbage(r4) & not sleep(r4)
   <- next4(slot);
      !check4(slots).
+!check4(slots).

+wakeup(r4,X,Y) 
   <- -+pos(last,X,Y);
      !at4(last);
      resume4(r4);
      !check4(slots).

@lg[atomic]
+garbage(r4) : not .desire(carry_to4(r2)) & not sleep(r4)
   <- !carry_to4(r2).

+!carry_to4(R): pos(r4,X,Y)
   <- // carry garbage to r2
      !take4(garb,R,X,Y).
+!carry_to4(R) <- !carry_to4(R).

+!take4(S,L,X,Y) : true
   <- !ensure_pick4(S);
      awake4(X,Y);
      !at4(L);
      drop4(S).

+!ensure_pick4(S) : garbage(r4)
   <- pick4(garb);
      !ensure_pick4(S).
+!ensure_pick4(_).

+!at4(L) : at4(L).
+!at4(L) : pos(L,X,Y) 
   <- move_towards4(X,Y);
      !at4(L).
+!at4(L) <- !at4(L).
