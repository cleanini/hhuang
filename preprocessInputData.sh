cp /data/lstm-result/trace/*.txt $1/.

rm $1/Event-only.txt $1/Event-only-uniq.txt 
cut -d',' -f2 $1/EventTrac.txt >> $1/Event-only.txt
uniq $1/Event-only.txt >> $1/Event-only-uniq.txt
cut -d';' -f1 $1/Event-only-uniq.txt >> $1/e-input.csv
cut -d';' -f3 $1/Event-only-uniq.txt >> $1/p1-input.csv
cut -d';' -f4 $1/Event-only-uniq.txt >> $1/p2-input.csv
rm $1/Event-only.txt
