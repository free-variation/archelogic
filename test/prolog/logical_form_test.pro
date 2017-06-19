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
	parse(0, _, Rels),
	logical_form(Rels, LF),
	lambda_reduce(LF, RedLF),
	RedLF = X^('John'=X, E^(run(E), subject(E, X))).

test(logical_form_iv_all) :-
	parse(5, _, Rels),
	logical_form(Rels, LF),
	lambda_reduce(LF, RedLF),
	RedLF = (\+cat(X); E^(run(E), subject(E, X))).

test(logical_form_iv_the) :-
	parse(6, _, Rels),
	logical_form(Rels, LF),
	lambda_reduce(LF, RedLF),
	RedLF = Y^((\+cat(X); X = Y), E^(run(E), subject(E, X))).


test(logical_form_iv_a) :-
	parse(7, _, Rels),
	logical_form(Rels, LF),
	lambda_reduce(LF, RedLF),
	RedLF = X^(cat(X), E^(run(E), subject(E, X))).

test(logical_form_iv_no) :-
	parse(8, _, Rels),
	logical_form(Rels, LF),
	lambda_reduce(LF, RedLF),
	RedLF = \+(X^(cat(X), E^(run(E), subject(E, X)))).

:- end_tests(logical_form).

run_all_parses :-
	findall(RedLF, (parse(_, _, Rels), logical_form(Rels, LF), lambda_reduce(LF, RedLF)), RedLFs),
	maplist(portray_clause, RedLFs).

