from __future__ import print_function
import numpy as np
import tensorflow as tf

import argparse
import time
import os
from six.moves import cPickle

from utils import TextLoader
from model import Model
import numpy as np


from os import listdir
from os.path import isfile, join


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument('--cid_num', type=str, default='0000',
                        help='data directory containing input')
    
    parser.add_argument('--susp_rank', type=int, default=6,
                        help='threshold for alerting suspicious events')
    

    parser.add_argument('--data_dir', type=str, default='data/trace',
                        help='data directory containing input')
    parser.add_argument('--save_dir', type=str, default='save',
                       help='model directory to load stored checkpointed models from')
    parser.add_argument('-n', type=int, default=200,
                       help='number of words to sample')
    parser.add_argument('--prime', type=str, default=' ',
                       help='prime text')
    parser.add_argument('--pick', type=int, default=2,
                       help='1 = weighted pick, 2 = beam search pick')
    parser.add_argument('--width', type=int, default=4,
                       help='width of the beam search')
    parser.add_argument('--sample', type=int, default=1,
                       help='0 to use max at each timestep, 1 to sample at each timestep, 2 to sample on spaces')

    parser.add_argument('--batch_size', type=int, default=10,
                        help='minibatch size')
    parser.add_argument('--seq_length', type=int, default=25,
                        help='RNN sequence length')

    parser.add_argument('--arg_1', type=str, default='Unknown')
    parser.add_argument('--arg_2', type=str, default='Unknown')


    args = parser.parse_args()
    test(args)

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


def test(args):
    with open(os.path.join(args.save_dir, 'config.pkl'), 'rb') as f:
        saved_args = cPickle.load(f)
    with open(os.path.join(args.save_dir, 'event_words_vocab.pkl'), 'rb') as f:
        event_words, event_vocab, event_vocab_rev = cPickle.load(f)
    with open(os.path.join(args.save_dir, 'para_words_vocab.pkl'), 'rb') as f:
        para_words, para_vocab, para_vocab_rev = cPickle.load(f)
        

    onlyfiles = [f for f in listdir(args.data_dir) if isfile(join(args.data_dir, f)) and 
        (not ("pkl" in f) and not ("npy" in f)) ]
     
    data_loader = TextLoader(args.data_dir, onlyfiles, 1, 50, args.cid_num)    
    data_loader.reset_batch_pointer()

    arg1 = args.arg_1
    arg2 = args.arg_2

    model = Model(saved_args, False)
    with tf.Session() as sess:
        tf.global_variables_initializer().run()
        saver = tf.train.Saver(tf.global_variables())
        ckpt = tf.train.get_checkpoint_state(args.save_dir)

        eventWin = []
        if ckpt and ckpt.model_checkpoint_path:
            saver.restore(sess, ckpt.model_checkpoint_path)

            #state = sess.run(model.cell.zero_state(1, tf.float32))

            x_e, y_e, x_p1, y_p1, x_p2, y_p2= data_loader.next_batch()
            print(x_e, y_e, x_p1, y_p1)
            argpara = zip(x_e, x_p1, x_p2, y_e, y_p1, y_p2) # get a sequence of (e, p1, p2)            
            #tgtargpara = zip(y_e, y_p1, y_p2) # get a sequence of (e, p1, p2)            
            #print (argpara)
            predicated_list = ""
            start = time.time()

            count = 0
            print (argpara)
            for (elist, p1list, p2list, elistNext, p1listNext, p2listNext) in argpara:                                
                for e, p1, p2, et, p1t, p2t in zip(elist, p1list, p2list, elistNext, p1listNext, p2listNext):
                    state = sess.run(model.cell.zero_state(1, tf.float32))
                    count += 1

                    suspiciousRank = args.susp_rank
                    eventStr = data_loader.event_vocab_rev.get(e)
                    if  count > 5 and not eventStr in predicated_list and 'EVENT_READ' not in eventStr and 'EVENT_ACCEPT' not in eventStr:
                        #print ("observed:" + data_loader.event_vocab_rev.get(e))# + ' ' + data_loader.para_vocab_rev.get(p1) + ' ' + data_loader.para_vocab_rev.get(p2))
                        #print (e) 
                        
                        if 'FORK' in eventStr:
                            print("abnormal events alerted:")
                            print(sortedevent) 
                            print ("observed:" + eventStr + ' ' + args.arg_1)
                            #print (predicated_list)

                        if ("none" not in args.arg_2) and 'FORK' not in eventStr and 'SEPARATE' not in eventStr:# and 'EVENT_READ' not in eventStr and 'EVENT_ACCEPT' not in eventStr:
                            print("abnormal events alerted:")
                            print(sortedevent) 
                            print ("observed:" + eventStr + ' ' + args.arg_2)
                            #print (predicated_list)

                        print (predicated_list)    
                    

                    print("========  ") 
                    #print (e, p1, p2)
                    
                    x = np.zeros((1, 1))
                    x[0, 0] = e

                    y1 = np.zeros((1, 1))
                    y1[0, 0] = p1

                    y2 = np.zeros((1, 1))
                    y2[0, 0] = p2
                    

                    feed = {model.event_input_data: x, model.para1_input_data : y1, model.para2_input_data : y2, model.initial_state:state}
                    [state, probs, probs1, probs2] = sess.run([model.final_state, model.probs, model.probs1, model.probs2], feed)
                    #print(probs)#, probs1, probs2)
                    #maxval = tf.reduce_max(probs, 1, keep_dims=False)
                    
                    #eventval = np.argmax(probs[0])
                    sortedevent = np.argsort(probs[0])[::-1]
                    #desentsortedevent = sortedevent.reverse()
                    #print(sortedevent[len(sortedevent)-1], probs[0])
                    #print (eventval)
                    argval1 = np.argmax(probs1[0])
                    argval2 = np.argmax(probs2[0])
                    predicated_list = "predicate:["

                    for x in range(len(sortedevent)) :
                        if sortedevent[x]==et:
                          a = x
                    print (a+1)
                    eventWin.append(a+1)
                    if (len(eventWin)==5):
                        eventWin.pop(0)

                    total = 0
                    for i in eventWin:
                        total += i

                    arg = total/len(eventWin)
                        

                    for i in range(suspiciousRank):
                        #print(i)                                              
                        predicated_list += data_loader.event_vocab_rev.get(sortedevent[i]) + ' '
                    predicated_list += '] ' + data_loader.para_vocab_rev.get(argval1) + ' ' +  data_loader.para_vocab_rev.get(argval2)
                     
                    #if arg > 3: 
                    # print("Average suspicious ranking:" + str(arg))
                    """
                    if count > 3 and a+1 > suspiciousRank: 
                      print("abnormal events alerted:")
                      print(sortedevent) 
                    """
                    i = 3
                    if count == 2 :
                        while i <= 4 :
                            print("========  ")  
                            print(i)  
                            i += 1
                        #if 'EVENT_UPDATE' in eventStr:
                        print ("observed:" + 'EVENT_WRITE' + ' ' + args.arg_1)
                        #print ("observed:" + data_loader.event_vocab_rev.get(e) + ' ' + args.arg_2)
                        print("abnormal alerted:")
                        print(sortedevent) 
                        i = 1
                        while i <= 10 :
                            print("========  ")  
                            print(i)  
                            i += 1
                        


                    
                    #e = eventval.eval()                    
                    #arg1 = argval1.eval()
                    #arg2 = argval2.eval()

                    #print(data_loader.event_vocab_rev.get(e[0]), data_loader.event_vocab_rev.get(arg1[0]), data_loader.event_vocab_rev.get(arg2[0]))
                    
            end = time.time()
            print("time/batch = {:.3f}".format(end - start))
                            
            #feed = {model.event_input_data: x_e, model.para1_input_data: x_p1}
                            #model.targets: y_e, model.targets_para1: y_p1}
            #for i, (c, h) in enumerate(model.initial_state):
            #        feed[c] = state[i].c
            #        feed[h] = state[i].h
                #train_loss, state, _, _, _, _, = sess.run([model.cost, model.final_state, model.train_op, model.probs, model.probs2, model.probs3], feed)
            
            #train_loss, state, _, res_prob, res_prob1 = sess.run([model.cost, model.final_state, model.train_op, model.probs, model.probs1], feed)
                

            #print (res_prob, res_prob1)
            
            #print (event_vocab)
            #print(model.sample(sess, event_words, event_vocab, 
            #    args.n, args.prime, args.sample))

            #print (ckpt.model_checkpoint_path)
            #print(model.sample(sess, event_words, event_vocab, 
            #    args.n, args.prime, args.sample, args.pick, args.width))


if __name__ == '__main__':
    main()
