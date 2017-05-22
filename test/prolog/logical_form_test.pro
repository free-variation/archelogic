:- begin_tests(logical_form).

test(parses) :-
	findall(Id, parse(Id, _, _), Ids),
	length(Ids, 5).

test(up) :-
	up(runs, [X]>>runs(X)).

test(up_down) :-
	up(runs, L),
	down(L, runs).

test(up_up) :-
	up(drinks, L), 
	f(L, water, M), 
	up(M, N), 
	f(N, john, drinks(water, john)).

test(down) :-
	down([_]>>runs(_), _).

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
	logical_form(Rels, 'runs').

test(logical_form_iv) :-
	parse(0, _, Rels),
	logical_form(Rels, runs('John')).

test(logical_form_tv) :-
	parse(3, _, Rels),
	logical_form(Rels, knows('Mary', 'John')).

test(f) :-
	up(runs, L),
	f(L, john, runs(john)).

test(f_existential) :-
	up(likes, L),
	f(L, {X}/(unicorn(X), green(X)), {X}/((unicorn(X), green(X)), likes(X))).

:- end_tests(logical_form).