lf(Id, RedLF) :-
	parse(Id, _, Rels),
	logical_form(Rels, LF),
	lambda_reduce(LF, RedLF).

:- begin_tests(logical_form).

test(relations_of_type) :-
	parse(0, _, Rels),
	relations_of_type('nsubj', Rels, [rel('nsubj',word('2','runs','run','VBZ'),word('1','John','John','NNP'))]),
	parse(1, _, Rels1),
	relations_of_type('nsubj', Rels1, [
		rel('nsubj',word('2','knows','know','VBZ'),word('1','John','John','NNP')),
		rel('nsubj',word('7','likes','like','VBZ'),word('6','he','he','PRP'))]),
	relations_of_type('nonsense', Rels1, []).

test(relations_for_governor) :-
	parse(0, _, Rels),
	relations_for_governor('1', Rels, []),
	relations_for_governor('2', Rels, [
		rel('nsubj', word('2', 'runs', 'run', 'VBZ'), word('1', 'John', 'John', 'NNP')), 
		rel('punct', word('2', 'runs', 'run', 'VBZ'), word('3', '.', '.', '.'))]).

test(logical_form_n) :-
	parse(2, _, Rels),
	logical_form(Rels, runs).

test(logical_form_iv) :-
	parse(0, _, Rels),
	logical_form(Rels, LF),
	LF = beta({}/[P]>>_X^('John'=X, beta(P, [X])), [{}/[Y]>>E^(run(E), subject(E, Y))]).

test(logical_form_iv_reduced) :-
	lf(0, RedLF),
	RedLF = X^('John'=X, E^(run(E), subject(E, X))).

test(logical_form_iv_all) :-
	lf(5, RedLF),
	RedLF = (\+cat(X); E^(run(E), subject(E, X))).

test(logical_form_iv_the) :-
	lf(6, RedLF),
	RedLF = Y^(\+cat(X); X = Y, E^(run(E), subject(E, X))).

test(logical_form_iv_a) :-
	lf(7, RedLF),
	RedLF = X^(cat(X), E^(run(E), subject(E, X))).

test(logical_form_iv_no1) :-
	lf(8, RedLF),
	RedLF = \+(X^(cat(X), E^(run(E), subject(E, X)))).

test(logical_form_iv_no2) :-
	lf(9, RedLF),
	RedLF = \+(X^(cat(X), E^(run(E), subject(E, X)))).

test(logical_form_tv_obj_a) :-
	lf(10, RedLF),
	RedLF = X^('Django'= X, E^(love(E), subject(E, X), Y^(cat(Y), object(E, Y)))).

test(logical_form_tv_obj_all) :-
	lf(11, RedLF),
	RedLF = X^('Django'= X, E^(love(E), subject(E, X), (\+cat(Y); object(E, Y)))).

test(logical_form_tv_obj_no) :-
	lf(12, RedLF),
	RedLF = X^('Django' = X, E^(love(E), subject(E, X), \+Y^(badcat(Y), object(E, Y)))).

test(logical_form_tv_the_the) :-
	lf(13, RedLF),
	RedLF = Y^(\+cat(X); X = Y, E^(lick(E), subject(E, X), Z^(\+mat(U); U = Z, object(E, U)))).

test(logical_form_tv_every_a) :-
	lf(14, RedLF),
	RedLF = (\+man(X); E^(love(E), subject(E, X), Y^(woman(Y), object(E, Y)))).

test(logical_form_tv_every_every) :-
	lf(15, RedLF),
	RedLF = (\+man(X); E^(love(E), subject(E, X), (\+woman(Y); object(E, Y)))).

test(logical_form_iv_pl_def_subj) :-
	lf(16, RedLF),
	RedLF = (\+cat(X); E^(run(E), subject(E, X))).

test(logical_form_iv_pl_iterated_dets) :-
	lf(17, RedLF),
	RedLF = (\+cat(X); E^(run(E), subject(E, X))).

test(logical_form_tv_the_the_pl) :-
	lf(18, RedLF),
	RedLF = Y^(\+cat(X); X = Y, E^(lick(E), subject(E, X), (\+mat(U); object(E, U)))).

test(relative_pronoun_that) :-
	relative_pronoun(that).
test(not_relative_pronoun_in) :-
	\+relative_pronoun(in).

test(relative_adverb_when) :-
	relative_adverb(when).
test(not_relative_adverb_how) :-
	\+relative_adverb(how).	
	
:- end_tests(logical_form).

run_all_parses :-
	findall(RedLF, (parse(_, _, Rels), logical_form(Rels, LF), lambda_reduce(LF, RedLF)), RedLFs),
	maplist(portray_clause, RedLFs).

