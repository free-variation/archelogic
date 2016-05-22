semantic_relation(hyp, [n, v], 2).
semantic_relation(hypo, [n, v], 2).
semantic_relation(ins, [n], 2).
semantic_relation(has_instance, [n], 2).
semantic_relation(ent, [v], 2).
semantic_relation(sim, [a], 2).
semantic_relation(mm, [n], 2).
semantic_relation(holo_mem, [n], 2).
semantic_relation(ms, [n], 2).
semantic_relation(holo_subs, [n], 2).
semantic_relation(mp, [n], 2).
semantic_relation(holo_part, [n], 2).
semantic_relation(cs, [v], 2).
semantic_relation(causee, [v], 2).
semantic_relation(vgp, [v], 4).
semantic_relation(ant, [v, n, a, r, s], 4).


hypo(Syn1, Syn2) :-
	hyp(Syn2, Syn1).

has_instance(Syn1, Syn2) :-
	ins(Syn2, Syn1).

holo_mem(Syn1, Syn2) :-
	mm(Syn2, Syn1).

holo_subs(Syn1, Syn2) :-
	ms(Syn2, Syn1).

holo_part(Syn1, Syn2) :-
	mp(Syn2, Syn1).

causee(Syn1, Syn2) :-
	cs(Syn2, Syn1).

% translation of wordnet relations to McCartney relations
relation_map(hyp, forward).
relation_map(hypo, reverse).
relation_map(mm, forward).
relation_map(holo_mem, reverse).  
relation_map(ms, forward).
relation_map(holo_subs, reverse).
relation_map(mp, forward).
relation_map(holo_part, reverse).
relation_map(ant, negation).
relation_map(vgp, equivalence).
relation_map(sim, equivalence).
relation_map(ins, forward).
relation_map(has_instance, reverse).
relation_map(ent, forward).
relation_map(syn, equivalence).

join(equivalence, X, X).
join(X, equivalence, X).
join(forward, forward, forward).
join(forward, negation, alternation).
join(forward, alternation, alternation).
join(reverse, reverse, reverse).
join(reverse, negation, cover).
join(reverse, cover, cover).
join(negation, forward, cover).
join(negation, reverse, alternation).
join(negation, negation, equivalence).
join(negation, alternation, reverse).
join(negation, cover, forward).
join(alternation, reverse, alternation).
join(alternation, negation, forward).
join(alternation, cover, forward).
join(cover, forward, cover).
join(cover, negation, reverse).



find_path(Word1, Word2, POS, MaxPathLength, Path) :-
	s(Syn1, WordNum1, Word1, POS, _, _),
	s(Syn2, WordNum2, Word2, POS, _, _),
	find_path(Syn1, WordNum1, Syn2, WordNum2, POS, MaxPathLength, [], Path).

find_path(Syn1, _, Syn1, WordNum2, _, _, Path1, Path2) :-
	s(Syn1, WordNum2, Word, _, _, _),
	append(Path1, [syn:Word], Path2).	

find_path(Syn1, WordNum1, Syn2, WordNum2, POS, _, Path1, Path2) :-
	semantic_relation(Relation, POSList, NumArgs),
	member(POS, POSList),
	(	NumArgs = 2 
	->	Call =.. [Relation, Syn1, Syn2]
	;	Call =.. [Relation, Syn1, WordNum1, Syn2, WordNum2]
	),
	call(Call),

	% if we have a relation between the words, then do not 
	% test paths that have this relation
	!,

	(	last(Path1, PrevRelation:_)
	->	(	relation_map(PrevRelation, R1),
			relation_map(Relation, R2),
			join(R1, R2, _)
		)
	;	true
	),

	%s(Syn2, WordNum2, Word, _, _, _),
	append(Path1, [Relation:Syn2], Path2).

find_path(Syn1, WordNum1, Syn2, WordNum2, POS, MaxPathLength, Path1, Path2) :-
	length(Path1, L),
	L < MaxPathLength,
	semantic_relation(Relation, POSList, NumArgs),
	member(POS, POSList),
	var(Syn3), 
	var(WordNum3),
	(	NumArgs = 2 
	->	Call =.. [Relation, Syn1, Syn3]
	;	Call =.. [Relation, Syn1, WordNum1, Syn3, WordNum3]
	),
	call(Call),

	
	(	last(Path1, PrevRelation:_)
	->	(	relation_map(PrevRelation, R1),
			relation_map(Relation, R2),
			join(R1, R2, _)
		)
	;	true
	),
	

	% s(Syn3, WordNum3, Word, _, _, _),
	append(Path1, [Relation:Syn3], Path3),
	find_path(Syn3, WordNum3, Syn2, WordNum2, POS, MaxPathLength, Path3, Path2).

translate_relation(Relation:_, Relation1) :-
	relation_map(Relation, Relation1).

translate_path(Path, Path1) :-
	maplist(translate_relation, Path, Path1).

reduce_path(Path, AggregateRelation) :-
	translate_path(Path, P1),
	reverse(P1, P2),
	foldl(join, P2, equality, AggregateRelation).