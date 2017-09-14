import collections
import sys
from os import walk

dict = {}
mypath = sys.argv[1]

for (dirpath, dirnames, filenames) in walk(mypath):
   # f.extend(filenames)
   for file in filenames: 
     if "eventSortedTrace" in file :
       pathfile = dirpath + file 
       print pathfile
       with open(pathfile) as f :
           for line in f :
		if  "invoked " in line :
			fds = line.split("  invoked")
			seq = fds[0].split(":")[0]
			dict[int(seq)] = fds[1]

od = collections.OrderedDict(sorted(dict.items()))
for k, v in od.items(): print(k, v)

