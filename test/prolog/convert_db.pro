convert_s(File) :-
	tell(File),
	forall(
		s(SynsetID, WordNum, Word, POS, SenseNumber, TagCount),
		format("[~a ~a \"~a\" :~a ~a ~a]\n", [SynsetID, WordNum, Word, POS, SenseNumber, TagCount])),
	told.

convert_pair(Database) :-
	atom_concat(Database, '.clj', File),
	atom_concat('data/', File, FilePath),
	tell(FilePath),

	%format("(defrelation ~a [a b]\n", [Database]),
	forall(
		(Term =.. [Database, SynsetID1, SynsetID2],
		call(Term)),
		format("[~a ~a]\n", [SynsetID1, SynsetID2])),
	%format(")\n"),
	told.

convert_four(Database) :-
	atom_concat(Database, '.clj', File),
	atom_concat('data/', File, FilePath),
	tell(FilePath),

	%format("(defrelation ~a [a b c d]\n", [Database]),
	forall(
		(Term =.. [Database, SynsetID1, WordNum1, SynsetID2, WordNum2],
		call(Term)),
		format("[~a ~a ~a ~a]\n", [SynsetID1, WordNum1, SynsetID2, WordNum2])),
	%format(")\n"),
	told.

convert_all :-
	convert_s('data/s.clj'),
	convert_four(ant),
	convert_pair(hyp),
	convert_pair(mm),
	convert_pair(ms),
	convert_pair(ins),
	convert_pair(mp),
	convert_pair(cs),
	convert_pair(ent),
	convert_pair(sim),
	convert_four(vgp).


