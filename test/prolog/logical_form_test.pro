:- begin_tests(logical_form).

test(parses) :-
	findall(Id, parse(Id, _, _), Ids),
	length(Ids, 5).

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
	logical_form(Rels, {X}/([runs(X), subject(X, 'John') | F]-F)). 

test(logical_form_tv) :-
	parse(3, _, Rels),
	logical_form(Rels, {X}/([knows(X), subject(X, 'John'), object(X, 'Mary') | F]-F)).

test(f) :-
	f(runs, john, runs(john)).

test(f_existential) :-
	f(likes, {X}/([unicorn(X), green(X) | F]-F), {X}/([unicorn(X), green(X), likes(X) | F]-F)).

test(logical_form_subject_relative) :-
	parse(4, _, Rels),
	logical_form(Rels, {X, Y, Z}/[knows(X), subject(X, 'John'), object(X, Y), Y = 'Mary', likes(Z), subject(Z, Y), object(Z, tea)]).

:- end_tests(logical_form).