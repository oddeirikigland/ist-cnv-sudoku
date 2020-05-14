# Python ≥3.5 is required
import sys
assert sys.version_info >= (3, 5)

# Scikit-Learn ≥0.20 is required
import sklearn
assert sklearn.__version__ >= "0.20"

# Common imports
import numpy as np
import os
import pandas as pd
import seaborn as sn

# To plot pretty figures
import matplotlib as mpl
import matplotlib.pyplot as plt
mpl.rc('axes', labelsize=14)
mpl.rc('xtick', labelsize=12)
mpl.rc('ytick', labelsize=12)

# Where to save the figures
PROJECT_ROOT_DIR = "."
CHAPTER_ID = "metric_plots"
IMAGES_PATH = os.path.join(PROJECT_ROOT_DIR, "images", CHAPTER_ID)
os.makedirs(IMAGES_PATH, exist_ok=True)

def save_fig(fig_id, tight_layout=True, fig_extension="png", resolution=300):
    path = os.path.join(IMAGES_PATH, fig_id + "." + fig_extension)
    print("Saving figure", fig_id)
    if tight_layout:
        plt.tight_layout()
    plt.savefig(path, format=fig_extension, dpi=resolution)

FILEPATH = 'cnv_sudoku_combined.csv'
data = pd.read_csv(FILEPATH)

# Removes (S) and (N) from column names
# as2 = 'parameters (S),anewarraycount (N),b_count (N),dyn_bb_count (N),dyn_instr_count (N),dyn_method_count (N),fieldloadcount (N),fieldstorecount (N),i (S),i_count (N),lastRequestForParams (S),loadcount (N),m_count (N),metric_value (N),multianewarraycount (N),n1 (N),n2 (N),newarraycount (N),newcount (N),s (S),storecount (N),un (N)'
# aa = as2.split(',')
# aa = [a[:-4] + ',' for a in aa]
# aa = "".join(aa)
# print(aa)