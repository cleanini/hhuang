from __future__ import print_function
import tensorflow as tf

import argparse
import time
import os
from six.moves import cPickle

from utils import TextLoader
from model import Model

from os import listdir
from os.path import isfile, join

def main():
    parser = argparse.ArgumentParser(
                        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    
    parser.add_argument('--cid_num', type=str, default='0000',
                        help='data directory containing input')
    parser.add_argument('--data_dir', type=str, default='data/trace',
                        help='data directory containing input')
    parser.add_argument('--save_dir', type=str, default='save',
                        help='directory to store checkpointed models')
    parser.add_argument('--log_dir', type=str, default='logs',
                        help='directory to store tensorboard logs')
    parser.add_argument('--rnn_size', type=int, default=128,
                        help='size of RNN hidden state')
    parser.add_argument('--num_layers', type=int, default=2,
                        help='number of layers in the RNN')
    parser.add_argument('--model', type=str, default='lstm',
                        help='rnn, gru, lstm, or nas')
    parser.add_argument('--batch_size', type=int, default=50,
                        help='minibatch size')
    parser.add_argument('--seq_length', type=int, default=50,
                        help='RNN sequence length')
    parser.add_argument('--num_epochs', type=int, default=50,
                        help='number of epochs')
    parser.add_argument('--save_every', type=int, default=1000,
                        help='save frequency')
    parser.add_argument('--grad_clip', type=float, default=5.,
                        help='clip gradients at this value')
    parser.add_argument('--learning_rate', type=float, default=0.002,
                        help='learning rate')
    parser.add_argument('--decay_rate', type=float, default=0.97,
                        help='decay rate for rmsprop')
    parser.add_argument('--output_keep_prob', type=float, default=1.0,
                        help='probability of keeping weights in the hidden layer')
    parser.add_argument('--input_keep_prob', type=float, default=1.0,
                        help='probability of keeping weights in the input layer')
    parser.add_argument('--gpu_mem', type=float, default=0.666,
                       help='%% of gpu memory to be allocated to this process. Default is 66.6%%')
    parser.add_argument('--init_from', type=str, default=None,
                        help="""continue training from saved model at this path. Path must contain files saved by previous training process:
                            'config.pkl'        : configuration;
                            'chars_vocab.pkl'   : vocabulary definitions;
                            'checkpoint'        : paths to model file(s) (created by tf).
                                                  Note: this file contains absolute paths, be careful when moving files around;
                            'model.ckpt-*'      : file(s) with model definition (created by tf)
                        """)
    args = parser.parse_args()
    train(args)

def calcuateProbDis(res_prob, res_prob1):
    prob = tf.reduce_sum(res_prob, 1, keep_dims=True)
    prob1 = tf.reduce_sum(res_prob1, 1, keep_dims=True)
    #prob2 = tf.reduce_sum(res_prob2, 1, keep_dims=True)
    result = tf.Session().run(prob)
    result1 = tf.Session().run(prob1)

    print(result)
    print(result.size)

    print(result1)
    print(result1.size)



def train(args):

    onlyfiles = [f for f in listdir(args.data_dir) if isfile(join(args.data_dir, f)) and 
        (not ("pkl" in f) and not ("npy" in f)) ]
    


    for f in onlyfiles:
        print(f)
    data_loader = TextLoader(args.data_dir, onlyfiles, args.batch_size, args.seq_length, args.cid_num)
    args.event_vocab_size = data_loader.event_vocab_size
    args.para_vocab_size = data_loader.para_vocab_size 
    #event_vocab_rev
    
    #print(data_loader.event_vocab)
    #print(data_loader.event_words)
    #for data_loader in data_loader_list:
    #    args.vocab_size = data_loader.vocab_size

    #data_loader = TextLoader(args.data_dir, args.batch_size, args.seq_length)
    
    # check compatibility if training is continued from previously saved model
    
    if args.init_from is not None:
        # check if all necessary files exist
        assert os.path.isdir(args.init_from)," %s must be a path" % args.init_from
        assert os.path.isfile(os.path.join(args.init_from,"config.pkl")),"config.pkl file does not exist in path %s"%args.init_from
        assert os.path.isfile(os.path.join(args.init_from,"event_words_vocab.pkl")),"words_vocab.pkl.pkl file does not exist in path %s" % args.init_from
        assert os.path.isfile(os.path.join(args.init_from,"para_words_vocab.pkl")),"words_vocab.pkl.pkl file does not exist in path %s" % args.init_from
        ckpt = tf.train.get_checkpoint_state(args.init_from)
        assert ckpt,"No checkpoint found"
        assert ckpt.model_checkpoint_path,"No model path found in checkpoint"

        # open old config and check if models are compatible
        with open(os.path.join(args.init_from, 'config.pkl'), 'rb') as f:
            saved_model_args = cPickle.load(f)
        
        need_be_same=["model","rnn_size","num_layers","seq_length"]
        for checkme in need_be_same:
            assert vars(saved_model_args)[checkme]==vars(args)[checkme],"Command line argument and saved model disagree on '%s' "%checkme
        #self.para_vocab, self.para_words, self.para_vocab_rev
        # open saved vocab/dict and check if vocabs/dicts are compatible
        with open(os.path.join(args.init_from, 'event_words_vocab.pkl'), 'rb') as f:
            event_saved_vocab, event_saved_words, event_saved_vocab_rev,  = cPickle.load(f)
        with open(os.path.join(args.init_from, 'para_words_vocab.pkl'), 'rb') as f:
            para_saved_vocab, para_saved_words, para_saved_vocab_rev,  = cPickle.load(f)
            
        assert event_saved_words==data_loader.event_words, "Data and loaded model disagree on word set!"
        assert event_saved_vocab==data_loader.event_vocab, "Data and loaded model disagree on dictionary mappings!"
        assert event_saved_vocab_rev==data_loader.event_vocab_rev, "Data and loaded model disagree on dictionary mappings!"
    
        assert para_saved_words==data_loader.para_words, "Data and loaded model disagree on word set!"
        assert para_saved_vocab==data_loader.para_vocab, "Data and loaded model disagree on dictionary mappings!"
        assert para_saved_vocab_rev==data_loader.para_vocab_rev, "Data and loaded model disagree on dictionary mappings!"
    


    
    with open(os.path.join(args.save_dir, 'config.pkl'), 'wb') as f:
        cPickle.dump(args, f)
    with open(os.path.join(args.save_dir, 'event_words_vocab.pkl'), 'wb') as f:
        cPickle.dump((data_loader.event_vocab, data_loader.event_words, data_loader.event_vocab_rev), f)
    with open(os.path.join(args.save_dir, 'para_words_vocab.pkl'), 'wb') as f:
        cPickle.dump((data_loader.para_vocab, data_loader.para_words, data_loader.para_vocab_rev), f)
    
         
    model = Model(args)
    gpu_options = tf.GPUOptions(per_process_gpu_memory_fraction=args.gpu_mem)

    with tf.Session(config=tf.ConfigProto(gpu_options=gpu_options)) as sess:
        # instrument for tensorboard
        summaries = tf.summary.merge_all()
        writer = tf.summary.FileWriter(
                os.path.join(args.log_dir, time.strftime("%Y-%m-%d-%H-%M-%S")))
        writer.add_graph(sess.graph)

        #sess.run(tf.global_variables_initializer())
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        # restore model
        if args.init_from is not None:
            saver.restore(sess, ckpt.model_checkpoint_path)
        for e in range(args.num_epochs):
            sess.run(tf.assign(model.lr, # the variable, a mutable tensor
                               args.learning_rate * (args.decay_rate ** e))) # variable to be assigned to the model.lr            
            data_loader.reset_batch_pointer()
            state = sess.run(model.initial_state)
            for b in range(data_loader.num_batches):
            #for y_e, y_a1, y_a2, b_e, b_a1, b_a2 in generate_batch():
                start = time.time()
                x_e, y_e, x_p1, y_p1, x_p2, y_p2 = data_loader.next_batch()

                feed = {model.event_input_data: x_e, model.para1_input_data: x_p1, model.para2_input_data: x_p2, 
                            model.targets: y_e, model.targets_para1: y_p1, model.targets_para2: y_p2}
                for i, (c, h) in enumerate(model.initial_state):
                    feed[c] = state[i].c
                    feed[h] = state[i].h
                #train_loss, state, _, _, _, _, = sess.run([model.cost, model.final_state, model.train_op, model.probs, model.probs2, model.probs3], feed)
                train_loss, state, _, res_prob, res_prob1, res_prob2 = sess.run([model.cost, model.final_state, model.train_op, model.probs, model.probs1, model.probs2], feed)
                
                print(res_prob, res_prob1, res_prob1)
                print("   ")
                #arrays = tf.constant(res_prob)
                #print (arrays[0, :])

                #arrays1 = tf.constant(res_prob1)
                #print (arrays1[0, :])
                
                #calcuateProbDis(res_prob, res_prob1)
                
                #for i in res_prob :

                #print(res_prob.size, res_prob1.size)
            
                # instrument for tensorboard
                summ, train_loss, state, _ = sess.run([summaries, model.cost, model.final_state, model.train_op], feed)
                writer.add_summary(summ, e * data_loader.num_batches + b)

                end = time.time()
                print("{}/{} (epoch {}), train_loss = {:.3f}, time/batch = {:.3f}" 
                      .format(e * data_loader.num_batches + b,
                              args.num_epochs * data_loader.num_batches,
                              e, train_loss, end - start))
                if (e * data_loader.num_batches + b) % args.save_every == 0\
                        or (e == args.num_epochs-1 and
                            b == data_loader.num_batches-1):
                    # save for the last result
                    checkpoint_path = os.path.join(args.save_dir, 'model.ckpt')
                    saver.save(sess, checkpoint_path,
                               global_step=e * data_loader.num_batches + b)
                    print("model saved to {}".format(checkpoint_path))
               
        
if __name__ == '__main__':
    main()

