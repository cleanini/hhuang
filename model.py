import tensorflow as tf
from tensorflow.contrib import rnn
from tensorflow.contrib import legacy_seq2seq
import random
import numpy as np
from beam import BeamSearch


class Model():
    def __init__(self, args, training=True):
        self.args = args
        if not training:
            args.batch_size = 1
            args.seq_length = 1

        if args.model == 'rnn':
            cell_fn = rnn.BasicRNNCell
        elif args.model == 'gru':
            cell_fn = rnn.GRUCell
        elif args.model == 'lstm':
            cell_fn = rnn.BasicLSTMCell
        elif args.model == 'nas':
            cell_fn = rnn.NASCell
        else:
            raise Exception("model type not supported: {}".format(args.model))

        cells = []
        for _ in range(args.num_layers):
            cell = cell_fn(args.rnn_size)
            if training and (args.output_keep_prob < 1.0 or args.input_keep_prob < 1.0):
                cell = rnn.DropoutWrapper(cell,
                                          input_keep_prob=args.input_keep_prob,
                                          output_keep_prob=args.output_keep_prob)
            cells.append(cell)

        self.cell = cell = rnn.MultiRNNCell(cells, state_is_tuple=True)

        self.event_input_data = tf.placeholder(
            tf.int32, [args.batch_size, args.seq_length])
        self.para1_input_data = tf.placeholder(
            tf.int32, [args.batch_size, args.seq_length]) 
        self.para2_input_data = tf.placeholder(
            tf.int32, [args.batch_size, args.seq_length]) 
        
        
        #self.para2_input_data = tf.placeholder(
        #    tf.int32, [args.batch_size, args.seq_length]) 
        


        self.targets = tf.placeholder(
            tf.int32, [args.batch_size, args.seq_length])
        self.targets_para1 = tf.placeholder(
            tf.int32, [args.batch_size, args.seq_length])
        self.targets_para2 = tf.placeholder(
            tf.int32, [args.batch_size, args.seq_length])


        #self.targets_para2 = tf.placeholder(
        #    tf.int32, [args.batch_size, args.seq_length])


        self.initial_state = cell.zero_state(args.batch_size, tf.float32)


        with tf.variable_scope('rnnlm'):
            softmax_w = tf.get_variable("softmax_w",
                                        [args.rnn_size, args.event_vocab_size])
            softmax_b = tf.get_variable("softmax_b", [args.event_vocab_size])

        with tf.variable_scope('rnnlm'):
            softmax_w1 = tf.get_variable("softmax_w1",
                                        [args.rnn_size, args.para_vocab_size])
            softmax_b1 = tf.get_variable("softmax_b1", [args.para_vocab_size])
            
        
        with tf.variable_scope('rnnlm'):
            softmax_w2 = tf.get_variable("softmax_w2",
                                        [args.rnn_size, args.para_vocab_size])
            softmax_b2 = tf.get_variable("softmax_b2", [args.para_vocab_size])
        

        #with tf.variable_scope('rnnlm3'):
        #    softmax_w3 = tf.get_variable("softmax_w3",
        #                                [args.rnn_size, para_vocab_size])
        #    softmax_b3 = tf.get_variable("softmax_b3", [args.para_vocab_size])
                
        
        event_embedding = tf.get_variable("event_embedding", [args.event_vocab_size, args.rnn_size])
        para_embedding = tf.get_variable("para_embedding", [args.para_vocab_size, args.rnn_size])

        event_inputs = tf.nn.embedding_lookup(event_embedding, self.event_input_data)
        para1_inputs = tf.nn.embedding_lookup(para_embedding, self.para1_input_data)
        para2_inputs = tf.nn.embedding_lookup(para_embedding, self.para2_input_data)

        inputs = tf.concat([event_inputs, para1_inputs, para2_inputs], 2) # for batch 

        # dropout beta testing: double check which one should affect next line
        if training and args.output_keep_prob:
            inputs = tf.nn.dropout(inputs, args.output_keep_prob)

        inputs = tf.split(inputs, args.seq_length, 1)
        inputs = [tf.squeeze(input_, [1]) for input_ in inputs] 

        
        def loop(prev, _): # //this is for the embedding training
            prev = tf.matmul(prev, softmax_w) + softmax_b 
            prev_symbol = tf.stop_gradient(tf.argmax(prev, 1))

            prev1 = tf.matmul(prev, softmax_w1) + softmax_b1 
            prev_symbol1 = tf.stop_gradient(tf.argmax(prev, 1))

            prev2 = tf.matmul(prev, softmax_w2) + softmax_b2 
            prev_symbol2 = tf.stop_gradient(tf.argmax(prev, 1))


            next_event_input =  tf.nn.embedding_lookup(event_embedding, prev_symbol)
            next_para1_input =  tf.nn.embedding_lookup(para_embedding, prev_symbol1)
            next_para2_input =  tf.nn.embedding_lookup(para_embedding, prev_symbol2)
            inputs = tf.concat([next_event_input, next_para1_input, next_para2_input], 2) # for batch 

            return inputs
             
        #https://github.com/tensorflow/tensorflow/blob/r1.3/tensorflow/contrib/legacy_seq2seq/python/ops/seq2seq.py 
        outputs, last_state = legacy_seq2seq.rnn_decoder(inputs, self.initial_state, cell, 
                                    loop_function=loop if not training else None, scope='rnnlm') # this is the model 

        #outputs1, last_state1 = legacy_seq2seq.rnn_decoder(inputs, self.initial_state, cell, 
        #                            loop_function=loop if not training else None, scope='rnnlm1')

        output = tf.reshape(tf.concat(outputs, 1), [-1, args.rnn_size])
        #output1 = tf.reshape(tf.concat(outputs, 1), [-1, args.rnn_size])

        self.logits = tf.matmul(output, softmax_w) + softmax_b #//
        self.probs = tf.nn.softmax(self.logits)

        self.logits1 = tf.matmul(output, softmax_w1) + softmax_b1 #//
        self.probs1 = tf.nn.softmax(self.logits1)

        self.logits2 = tf.matmul(output, softmax_w2) + softmax_b2 #//
        self.probs2 = tf.nn.softmax(self.logits2)


        loss1 = legacy_seq2seq.sequence_loss_by_example(
                [self.logits],
                [tf.reshape(self.targets, [-1])],
                [tf.ones([args.batch_size * args.seq_length])])

        loss_para1 = legacy_seq2seq.sequence_loss_by_example(
                [self.logits1],
                [tf.reshape(self.targets_para1, [-1])],
                [tf.ones([args.batch_size * args.seq_length])])
        
        loss_para2 = legacy_seq2seq.sequence_loss_by_example(
                [self.logits2],
                [tf.reshape(self.targets_para2, [-1])],
                [tf.ones([args.batch_size * args.seq_length])])
        
        loss = loss1 + loss_para1 + loss_para2


        self.cost = tf.reduce_sum(loss) / args.batch_size / args.seq_length
        with tf.name_scope('cost'):
            self.cost = tf.reduce_sum(loss) / args.batch_size / args.seq_length
        self.final_state = last_state
        self.lr = tf.Variable(0.0, trainable=False)
        tvars = tf.trainable_variables()
        grads, _ = tf.clip_by_global_norm(tf.gradients(self.cost, tvars),
                args.grad_clip)
        with tf.name_scope('optimizer'):
            optimizer = tf.train.AdamOptimizer(self.lr)
        self.train_op = optimizer.apply_gradients(zip(grads, tvars))

        # instrument tensorboard
        tf.summary.histogram('logits', self.logits)
        tf.summary.histogram('logits1', self.logits)
        tf.summary.histogram('logits2', self.logits)
        tf.summary.histogram('total loss', loss)
        tf.summary.scalar('train_loss', self.cost)

    """
    def sample(self, sess, chars, vocab, num=200, prime='EVENT_CLOSE ', sampling_type=1):
        state = sess.run(self.cell.zero_state(1, tf.float32))
        print(prime[:-1])
        
        for char in prime[:-1]:
            print(char)
    
           
            x = np.zeros((1, 1))
            x[0, 0] = vocab[char]
            feed = {self.input_data: x, self.initial_state: state}
            [state] = sess.run([self.final_state], feed)

        def weighted_pick(weights):
            t = np.cumsum(weights)
            s = np.sum(weights)
            return(int(np.searchsorted(t, np.random.rand(1)*s)))

        ret = prime
        char = prime[-1]
        for n in range(num):
            x = np.zeros((1, 1))

            x[0, 0] = vocab[char]
            feed = {self.input_data: x, self.initial_state: state}
            [probs, state] = sess.run([self.probs, self.final_state], feed)
            p = probs[0]

            if sampling_type == 0:
                sample = np.argmax(p)
            elif sampling_type == 2:
                if char == ' ':
                    sample = weighted_pick(p)
                else:
                    sample = np.argmax(p)
            else:  # sampling_type == 1 default:
                sample = weighted_pick(p)

            pred = chars[sample]
            ret += pred
            char = pred
        return ret
        """


    
    
    def sample(self, sess, words, vocab, num=200, prime='EVENT_CLOSE EVENT_WRITE', sampling_type=1, pick=1, width=4):
        def weighted_pick(weights):
            t = np.cumsum(weights)
            s = np.sum(weights)
            return(int(np.searchsorted(t, np.random.rand(1)*s)))

        def beam_search_predict(sample, state):
            #Returns the updated probability distribution (`probs`) and
            #`state` for a given `sample`. `sample` should be a sequence of
            #vocabulary labels, with the last word to be tested against the RNN.
            
            x = np.zeros((1, 1))
            x[0, 0] = sample[-1]
            feed = {self.event_input_data: x, self.initial_state: state}

            #print(x)
            #print(state)
            [probs, final_state] = sess.run([self.probs, self.final_state],feed)
            return probs, final_state
        
        
        def beam_search_pick(prime, width):
            #Returns the beam search pick.
            if not len(prime) or prime == ' ':
                prime = random.choice(list(vocab.keys()))
            prime_labels = [vocab.get(word, 0) for word in prime.split()]
            print(prime_labels)
            bs = BeamSearch(beam_search_predict, # take this method to compute
                           sess.run(self.cell.zero_state(1, tf.float32)),
                           prime_labels)
            samples, scores = bs.search(None, None, k=width, maxsample=num)
            return samples[np.argmin(scores)]

        ret = ''
        if pick == 1:
            state = sess.run(self.cell.zero_state(1, tf.float32))
            if not len(prime) or prime == ' ':
                prime  = random.choice(list(vocab.keys()))
            #print (prime)
            for word in prime.split()[:-1]:
                print (word)
                x = np.zeros((1, 1))
                x[0, 0] = vocab.get(word,0)
                feed = {self.event_input_data: x, self.initial_state:state}
                [state] = sess.run([self.final_state], feed)

            ret = prime
            word = prime.split()[-1]
            for n in range(num):
                x = np.zeros((1, 1))
                x[0, 0] = vocab.get(word, 0)
                feed = {self.event_input_data: x, self.initial_state:state}
                [probs, state] = sess.run([self.probs, self.final_state], feed)
                p = probs[0]

                if sampling_type == 0:
                    sample = np.argmax(p)
                elif sampling_type == 2:
                    if word == '\n':
                        sample = weighted_pick(p)
                    else:
                        sample = np.argmax(p)
                else: # sampling_type == 1 default:
                    sample = weighted_pick(p)

                pred = words[sample]
                ret += ' ' + pred
                word = pred
        elif pick == 2:
            pred = beam_search_pick(prime, width)
            for i, label in enumerate(pred):
                ret += ' ' + words[label] if i > 0 else words[label]
        return ret
        
    
