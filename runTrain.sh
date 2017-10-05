mkdir $1/$2-model 
python train.py --data_dir $1 --seq_length 10 --batch_size 5 --cid_num $2 --save_dir $1/$2-model 
