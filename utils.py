import codecs
import os
import collections
from six.moves import cPickle
import numpy as np
import tensorflow as tf


class TextLoader():
    def __init__(self, data_dir, file_list, batch_size, seq_length, encoding=None):
        self.data_dir = data_dir
        self.batch_size = batch_size
        self.seq_length = seq_length
        self.encoding = encoding

        onlyfiles = [f for f in file_list]

        for f in onlyfiles:
            if "event" in f :
                self.event_input_vocab_file = os.path.join(data_dir, f)
                self.event_vocab_file = os.path.join(data_dir, "event-vocab.pkl")
                #self.event_tensor_file = os.path.join(data_dir, self.event_input_file + "event-data.npy")
            if "para" in f:    
                self.para_input_vocab_file = os.path.join(data_dir, f)
                self.para_vocab_file = os.path.join(data_dir, "para-vocab.pkl")
                #self.para_tensor_file = os.path.join(data_dir, self.para_input_file + "para-data.npy")
           

            if "e-" in f and "input" in f:
                self.event_input_file = os.path.join(data_dir, f)
                self.event_tensor_file = os.path.join(data_dir, self.event_input_file + "event-data.npy")
            if "p1-" in f and "input" in f:
                self.para1_input_file = os.path.join(data_dir, f)
                self.para1_tensor_file = os.path.join(data_dir, self.para1_input_file + "para1-data.npy")
            if "p2-" in f and "input" in f:
                self.para2_input_file = os.path.join(data_dir, f)
                self.para2_tensor_file = os.path.join(data_dir, self.para2_input_file + "para2-data.npy")
            
                      


        if not (os.path.exists(self.event_vocab_file)):
            print("reading event textVocab file")
            self.preprocess2Vocab(self.event_input_vocab_file, self.event_vocab_file)
        else:
            print("loading event preprocessedVocab files")
            self.load_preprocessedVocab(self.event_vocab_file)

        if not (os.path.exists(self.para_vocab_file)):
            print("reading para textVocab file")
            self.preprocess2Vocab(self.para_input_vocab_file, self.para_vocab_file)
        else:
            print("loading para preprocessedVocab files")
            self.load_preprocessedVocab(self.para_vocab_file)    



        
        if not (os.path.exists(self.event_tensor_file)):
            print("self.event_tensor_file" + self.event_tensor_file)
            print("reading event raw input file")
            self.preprocess2Tensor(self.event_input_file, self.event_vocab_file, self.event_tensor_file)
        else:
            print("self.event_tensor_file" + self.event_tensor_file)
            print("loading event preprocessed tensor files")
            self.load_preprocessedTensor(self.event_vocab_file, self.event_tensor_file)
        

        if not (os.path.exists(self.para1_tensor_file)):
            print("self.para1_tensor_file" + self.para1_tensor_file)
            print("reading para1 raw input file")
            self.preprocess2Tensor(self.para1_input_file, self.para_vocab_file, self.para1_tensor_file)
        else:
            print("self.para1_tensor_file" + self.para1_tensor_file)
            print("loading para1 preprocessed tensor files")
            self.load_preprocessedTensor(self.para_vocab_file, self.para1_tensor_file)       

        if not (os.path.exists(self.para2_tensor_file)):
            print("self.para2_tensor_file" + self.para2_tensor_file)
            print("reading para2 raw input file")
            self.preprocess2Tensor(self.para2_input_file, self.para_vocab_file, self.para2_tensor_file)
        else:
            print("self.para2_tensor_file" + self.para2_tensor_file)
            print("loading para2 preprocessed tensor files")
            self.load_preprocessedTensor(self.para_vocab_file, self.para2_tensor_file)       
        
        


        self.create_batches()
        self.reset_batch_pointer()

    def build_vocab(self, sentences):
        """
        Builds a vocabulary mapping from word to index based on the sentences.
        Returns vocabulary mapping and inverse vocabulary mapping.
        """
        # Build vocabulary
        word_counts = collections.Counter(sentences)
        # Mapping from index to word
        vocabulary_inv = [x[0] for x in word_counts.most_common()]
        vocabulary_inv = list(sorted(vocabulary_inv))        
        # Mapping from word to index

        vocabulary = {x: i for i, x in enumerate(vocabulary_inv)}
        #print(vocabulary)

        #count = collections.Counter(sentences).most_common()
        #dictionary = dict()
        #for word, _ in count:
        #    dictionary[word] = len(dictionary)
        reverse_vocabulary = dict(zip(vocabulary.values(), vocabulary.keys()))

        return [vocabulary, vocabulary_inv, reverse_vocabulary]

    def build_dic(self, sentences):
        count = collections.Counter(sentences).most_common()
        dictionary = dict()
        for word, _ in count:
            dictionary[word] = len(dictionary)
        reverse_dictionary = dict(zip(dictionary.values(), dictionary.keys()))
        return [dictionary, reverse_dictionary]    


    def preprocess2Tensor(self, input_file, vocab_file, tensor_file):
        with codecs.open(input_file, "r", encoding=self.encoding) as f:
            data = f.read()        
        #counter = collections.Counter(data) # Counter({2: 4, 3: 4, 1: 3, 4: 2, 5: 1})
        #count_pairs = sorted(counter.items(), key=lambda x: -x[1]) # sort based on key counts
        if "e-" in input_file and "input" in input_file:
            event_text = data.split()
            #self.event_vocab, self.event_words = self.build_vocab(x_text)
            #print(self.event_vocab, self.event_words)
            #self.event_vocab_size = len(self.event_vocab)
            # self.event_vocab

            #with open(vocab_file, 'wb') as f:
            #    cPickle.dump(self.event_words, f)
            #The same operation like this [self.vocab[word] for word in x_text]
            # index of words as our basic data
            list_event = []
            print("test1")
            counter = 0
            for event in event_text:
                #counter += 1
                if self.event_vocab.has_key(event):
                    #print("event")                    
                    list_event.append(self.event_vocab.get(event))
                else :
                    #print("event1")
                    list_event.append(0)
            
            self.event_tensor = np.array(list_event)    
            #print(self.event_tensor)
            # Save the data to data.npy
            np.save(tensor_file, self.event_tensor)

            
        elif "p1-" in input_file and "input" in input_file:   
            p_text = data.split()
            print("p1-")
            #self.para_vocab, self.para_words = self.build_vocab(x_text)
            #print(self.para_vocab, self.para_words)
            #self.para_vocab_size = len(self.para_words)

            #with open(self.para_vocab_file, 'wb') as f:
            #    cPickle.dump(self.para_words, f)
            #The same operation like this [self.vocab[word] for word in x_text]
            # index of words as our basic data
            #self.para_tensor = np.array(list(map(self.para_vocab.get, p_text)))
            list_para = []
            for p in p_text:
                if self.para_vocab.has_key(p):
                    list_para.append(self.para_vocab.get(p))
                else :
                    list_para.append(0)
            
            self.para1_tensor = np.array(list_para)    
            np.save(tensor_file, self.para1_tensor)


        elif "p2-" in input_file and "input" in input_file:   
            p_text = data.split()
            print("p2-")
            #self.para_vocab, self.para_words = self.build_vocab(x_text)
            #print(self.para_vocab, self.para_words)
            #self.para_vocab_size = len(self.para_words)

            #with open(self.para_vocab_file, 'wb') as f:
            #    cPickle.dump(self.para_words, f)
            #The same operation like this [self.vocab[word] for word in x_text]
            # index of words as our basic data
            #self.para_tensor = np.array(list(map(self.para_vocab.get, p_text)))
            list_para = []
            for p in p_text:
                if self.para_vocab.has_key(p):
                    list_para.append(self.para_vocab.get(p))
                else :
                    list_para.append(0)
            
            self.para2_tensor = np.array(list_para)    
            np.save(tensor_file, self.para2_tensor)   

        

    
    def load_preprocessedTensor(self, vocab_file, tensor_file):
        if "e-" in tensor_file:
            #with open(vocab_file, 'rb') as f:
            #    self.event_chars = cPickle.load(f)
            #self.event_vocab_size = len(self.event_chars)            
            #self.event_vocab = dict(zip(self.event_chars, range(len(self.event_chars))))            
            self.event_tensor = np.load(tensor_file)
            #print("e:")
            #print(self.event_tensor) 
            self.num_batches = int(self.event_tensor.size / (self.batch_size * self.seq_length))

        elif "p1-" in tensor_file :   
            #with open(vocab_file, 'rb') as f:
            #    self.para_words = cPickle.load(f)
            #self.para_vocab_size = len(self.para_words)
            #self.para_vocab = dict(zip(self.para_words, range(len(self.para_words))))            
            self.para1_tensor = np.load(tensor_file)
            #print("p:")
            #print(self.para_tensor) 
            self.num_batches = int(self.para1_tensor.size / (self.batch_size * self.seq_length))    


        elif "p2-" in tensor_file :   
            #with open(vocab_file, 'rb') as f:
            #    self.para_words = cPickle.load(f)
            #self.para_vocab_size = len(self.para_words)
            #self.para_vocab = dict(zip(self.para_words, range(len(self.para_words))))            
            self.para2_tensor = np.load(tensor_file)
            #print("p:")
            #print(self.para_tensor) 
            self.num_batches = int(self.para2_tensor.size / (self.batch_size * self.seq_length))    


    def preprocess2Vocab(self, input_file, vocab_file):
        with codecs.open(input_file, "r", encoding=self.encoding) as f:
            data = f.read()
        
        #counter = collections.Counter(data) # Counter({2: 4, 3: 4, 1: 3, 4: 2, 5: 1})
        #count_pairs = sorted(counter.items(), key=lambda x: -x[1]) # sort based on key counts
        if "event" in input_file:
            event_text = data.split()
            self.event_vocab, self.event_words, self.event_vocab_rev = self.build_vocab(event_text)
            #print(self.event_vocab, self.event_words, self.event_vocab_rev)
            #self.event_vocab, self.event_words = self.build_dic(event_text)
            #print(self.event_vocab, self.event_words)
            self.event_vocab_size = len(self.event_words)

            with open(vocab_file, 'wb') as f:
                cPickle.dump(self.event_words, f)
            #The same operation like this [self.vocab[word] for word in x_text]
            # index of words as our basic data
            #self.event_tensor = np.array(list(map(self.event_vocab.get, x_text)))
            # Save the data to data.npy
            #np.save(tensor_file, self.event_tensor)
            
        elif "para" in input_file :   
            para_text = data.split()
            self.para_vocab, self.para_words, self.para_vocab_rev = self.build_vocab(para_text)
            #print(self.para_vocab, self.para_words, para_vocab_rev)
            self.para_vocab_size = len(self.para_words)

            with open(vocab_file, 'wb') as f:
                cPickle.dump(self.para_words, f)

            #The same operation like this [self.vocab[word] for word in x_text]
            # index of words as our basic data
            #self.para_tensor = np.array(list(map(self.para_vocab.get, x_text)))
            # Save the data to data.npy
            #np.save(tensor_file, self.para_tensor)
    

    def load_preprocessedVocab(self, vocab_file):
        if "event" in vocab_file:
            with open(vocab_file, 'rb') as f:
                self.event_words = cPickle.load(f)
            self.event_vocab_size = len(self.event_words)  
            self.event_vocab, self.event_words, self.event_vocab_rev = self.build_vocab(self.event_words)          
            #self.event_vocab = dict(zip(self.event_words, range(len(self.event_words))))            

            #self.event_tensor = np.load(self.event_tensor_file)
            #print(self.event_words) 
            #self.num_batches = int(self.event_tensor.size / (self.batch_size * self.seq_length))
        elif "para" in vocab_file :   
            with open(vocab_file, 'rb') as f:
                self.para_words = cPickle.load(f)
            self.para_vocab_size = len(self.para_words)
            self.para_vocab, self.para_words, self.para_vocab_rev = self.build_vocab(self.para_words)          
            #self.para_vocab = dict(zip(self.para_words, range(len(self.para_words))))            
            #self.para_tensor = np.load(self.para_tensor_file)
            #print(self.para_words) 
            #self.num_batches = int(self.para_tensor.size / (self.batch_size * self.seq_length))


    """
    onlyinputfiles = [f for f in listdir(args.data_dir) if isfile(join(args.data_dir, f)) and "input" in f]

    #filenames = [] # os.walk
    def generate_batch():
        for file in onlyinputfiles:            
            events = []
            args1 = []
            args2 = []
            for i in range(len(events)):
                b_e = events[i:i+args.seq_length]
                b_args1_e = args1[i:i+args.seq_length]
                b_args2_e = args2[i:i+args.seq_length]

                y_e = events[i+args.seq_length]
                y_args1_e = args1[i+args.seq_length]
                y_args2_e = args2[i+args.seq_length]
                # padd with 0000 
                yield y_e, y_args1_e, y_args2_e, b_e, b_args1_e, b_args2_e
    """

    def create_batches(self): # needs to concat two  

        #if self.event_tensor.size ==  self.para_tensor.size:
        self.num_batches = int(self.event_tensor.size / (self.batch_size * self.seq_length))

        # When the data (tensor) is too small,
        # let's give them a better error message
        if self.num_batches == 0:
            assert False, "Not enough data. Make seq_length and batch_size small."

        self.event_tensor = self.event_tensor[:self.num_batches * self.batch_size * self.seq_length]
        self.para1_tensor = self.para1_tensor[:self.num_batches * self.batch_size * self.seq_length]
        self.para2_tensor = self.para2_tensor[:self.num_batches * self.batch_size * self.seq_length]
        
        
        xdata = self.event_tensor  # x = [1, 3, 4]
        ydata = np.copy(self.event_tensor) 
        ydata[:-1] = xdata[1:]         
        ydata[-1] = xdata[0] # y = [3, 4, 1]
        

        xdata1 = self.para1_tensor  # x = [1, 3, 4]
        ydata1 = np.copy(self.para1_tensor) 
        ydata1[:-1] = xdata1[1:]         
        ydata1[-1] = xdata1[0] # y = [3, 4, 1]
                
        
        xdata2 = self.para2_tensor  # x = [1, 3, 4]
        ydata2 = np.copy(self.para2_tensor) 
        ydata2[:-1] = xdata2[1:]         
        ydata2[-1] = xdata2[0] # y = [3, 4, 1]
        

        #print(xdata.size)       
        #print(xdata1.size)
        #print(xdata2.size)
    
        #joined = tf.concat([xdata, xdata1], 0)
        #yjoined = np.copy(joined) 
        
        #inputs = tf.concat([event_inputs, para_inputs, para2_inputs], 2)
        #yjoined[:-1] = joined[1:]         
        #yjoined[-1] = joined[0]
        #print(xdata)
        #print(ydata)

        #//
        self.x_batches = np.split(xdata.reshape(self.batch_size, -1), 
                                  self.num_batches, 1)
        self.y_batches = np.split(ydata.reshape(self.batch_size, -1),
                                  self.num_batches, 1)

        self.x1_batches = np.split(xdata1.reshape(self.batch_size, -1), 
                                  self.num_batches, 1)
        self.y1_batches = np.split(ydata1.reshape(self.batch_size, -1),
                                  self.num_batches, 1)


        self.x2_batches = np.split(xdata2.reshape(self.batch_size, -1), 
                                  self.num_batches, 1)
        self.y2_batches = np.split(ydata2.reshape(self.batch_size, -1),
                                  self.num_batches, 1)



        """
        print(self.x_batches)
        print(self.y_batches)
        print(self.x1_batches)
        print(self.y1_batches)
        """
        
    def next_batch(self):
        x, y, x1, y1, x2, y2 = self.x_batches[self.pointer], self.y_batches[self.pointer], self.x1_batches[self.pointer], self.y1_batches[self.pointer], self.x2_batches[self.pointer], self.y2_batches[self.pointer]
        self.pointer += 1
        return x, y, x1, y1, x2, y2

    def reset_batch_pointer(self):
        self.pointer = 0
