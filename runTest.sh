rm $1model-$2.txt
echo "Loading trained model to test the new trace...." 
python test.py --data_dir /data/result/09-29-17-01-31 --save_dir /data/result/09-29-17-01-31/$1-model/  --cid_num $2  --susp_rank $3 >> $1model-$2.txt
#cat $1model-$2.txt
echo "rendering result...."
sublime $1model-$2.txt
