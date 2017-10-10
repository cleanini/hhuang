cp /home/darpa/lstm-detector/*All.txt $1/.
cp /home/darpa/lstm-detector/*.pkl $1/.
mkdir $1/$2-model 
python train.py --data_dir $1 --seq_length 100 --batch_size 5 --cid_num $2 --save_dir $1/$2-model 
