:- begin_tests(logical_form).

test(parses) :-
	findall(Id, parse(Id, _, _), Ids),
	length(Ids, 6).

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

:- end_tests(logical_form).