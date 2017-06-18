parse(0,"John runs.", [
	rel('root',word('0','','',''),word('2','runs','run','VBZ')),
	rel('nsubj',word('2','runs','run','VBZ'),word('1','John','John','NNP')),
	rel('punct',word('2','runs','run','VBZ'),word('3','.','.','.')),0]).
parse(1,"John knows a unicorn that he likes.", [
	rel('root',word('0','','',''),word('2','knows','know','VBZ')),
	rel('nsubj',word('2','knows','know','VBZ'),word('1','John','John','NNP')),
	rel('dobj',word('2','knows','know','VBZ'),word('4','unicorn','unicorn','NN')),
	rel('punct',word('2','knows','know','VBZ'),word('8','.','.','.')),
	rel('det',word('4','unicorn','unicorn','NN'),word('3','a','a','DT')),
	rel('acl:relcl',word('4','unicorn','unicorn','NN'),word('7','likes','like','VBZ')),
	rel('dobj',word('7','likes','like','VBZ'),word('5','that','that','IN')),
	rel('nsubj',word('7','likes','like','VBZ'),word('6','he','he','PRP')),1]).
parse(2,"runs.", [
	rel('root',word('0','','',''),word('1','runs','run','NNS')),
	rel('punct',word('1','runs','run','NNS'),word('2','.','.','.')),2]).
parse(3,"John knows Mary.", [
	rel('root',word('0','','',''),word('2','knows','know','VBZ')),
	rel('nsubj',word('2','knows','know','VBZ'),word('1','John','John','NNP')),
	rel('dobj',word('2','knows','know','VBZ'),word('3','Mary','Mary','NNP')),
	rel('punct',word('2','knows','know','VBZ'),word('4','.','.','.')),3]).
parse(4,"John knows Mary who drinks tea.", [
	rel('root',word('0','','',''),word('2','knows','know','VBZ')),
	rel('nsubj',word('2','knows','know','VBZ'),word('1','John','John','NNP')),
	rel('dobj',word('2','knows','know','VBZ'),word('3','Mary','Mary','NNP')),
	rel('punct',word('2','knows','know','VBZ'),word('7','.','.','.')),
	rel('acl:relcl',word('3','Mary','Mary','NNP'),word('5','drinks','drink','VBZ')),
	rel('nsubj',word('5','drinks','drink','VBZ'),word('4','who','who','WP')),
	rel('dobj',word('5','drinks','drink','VBZ'),word('6','tea','tea','NN')),4]).
parse(5,"All cats run.", [
	rel('root',word('0','','',''),word('3','run','run','VBP')),
	rel('det',word('2','cats','cat','NNS'),word('1','All','all','DT')),
	rel('nsubj',word('3','run','run','VBP'),word('2','cats','cat','NNS')),
	rel('punct',word('3','run','run','VBP'),word('4','.','.','.')),5]).
