import argparse
import os
import sys


def generate_command(class_path, specific_arg_arr):
    jar = 'java -cp target/pedigree-reconstruction-1.0-SNAPSHOT-standalone.jar'
    return '%s %s %s' % (jar, class_path, ' '.join(specific_arg_arr))


def main():
    """
    Run PREPARE pedigree-reconstruction tool suit.

    python prepare.py pedigree_reconstruction
    python prepare.py simulate_pedigree
    python prepare.py calculate_ibd_loss
    python prepare.py compare_pedigrees
    """
    parser = argparse.ArgumentParser(description=__doc__, prog=sys.argv[0])
    parser.add_argument('program', choices=['pedigree_reconstruction', 'simulate_pedigree', 'calculate_ibd_loss', 'compare_pedigrees'])

    known_args, specific_arg_arr = parser.parse_known_args(sys.argv[1:])
    program = known_args.program

    print(program)
    if program == 'pedigree_reconstruction':
        command = generate_command('prepare.pedreconstruction.PedigreeReconstructor', specific_arg_arr)
    elif program == 'simulate_pedigree':
        command = generate_command('prepare.simulator.WrightFisherSimulator', specific_arg_arr)
    elif program == 'calculate_ibd_loss':
        command = generate_command('prepare.evaluation.PedigreeIBDLossCalculator', specific_arg_arr)
    elif program == 'compare_pedigrees':
        command = generate_command('prepare.evaluation.PedigreeMinDistanceScorer', specific_arg_arr)

    print(command)
    os.system(command)

if __name__ == '__main__':
    main()


