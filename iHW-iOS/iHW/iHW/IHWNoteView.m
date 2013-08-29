//
//  IHWNoteView.m
//  iHW
//
//  Created by Jonathan Burns on 8/21/13.
//  Copyright (c) 2013 Jonathan Burns. All rights reserved.
//

#import "IHWNoteView.h"
#import "IHWScheduleViewController.h"
#import "IHWAppDelegate.h"

#define NOTE_HEIGHT 24
#define BUTTON_WIDTH 24
#define IMPORTANT_NOTE_HEIGHT 30

@implementation IHWNoteView {
    BOOL focused;
}

@synthesize note = _note;

- (id)initWithNote:(IHWNote *)note index:(int)noteIndex cellView:(IHWPeriodCellView *)cellView
{
    CGRect frame = CGRectMake(0, 0, cellView.notesView.bounds.size.width, NOTE_HEIGHT);
    if (cellView.notesView.subviews.count > 0){
        IHWNoteView *previous = [cellView.notesView.subviews objectAtIndex:cellView.notesView.subviews.count-1];
        CGFloat yPos = previous.frame.origin.y+previous.neededHeight;
        frame = CGRectMake(4, yPos, cellView.notesView.bounds.size.width, 0);
    }
    self = [super initWithFrame:frame];
    if (self) {
        //self.backgroundColor = [UIColor yellowColor];
        
        self.index = noteIndex;
        self.delegate = cellView;
        focused = NO;
        
        self.checkbox = [[UIButton alloc] initWithFrame:CGRectMake(0, 0, 0, NOTE_HEIGHT)];
        [self.checkbox setImage:[UIImage imageNamed:@"checkboxUnchecked"] forState:UIControlStateNormal];
        [self.checkbox setImage:[UIImage imageNamed:@"checkboxUnchecked"] forState:UIControlStateSelected|UIControlStateHighlighted];
        [self.checkbox setImage:[UIImage imageNamed:@"checkboxChecked"] forState:UIControlStateSelected];
        [self.checkbox setImage:[UIImage imageNamed:@"checkboxChecked"] forState:UIControlStateHighlighted];
        [self addSubview:self.checkbox];
        [self.checkbox addTarget:self action:@selector(toggleChecked) forControlEvents:UIControlEventTouchUpInside];
        
        self.textField = [[UITextField alloc] initWithFrame:CGRectZero];
        [self addSubview:self.textField];
        self.textField.adjustsFontSizeToFitWidth = YES;
        self.textField.minimumFontSize = 12;
        self.textField.placeholder = @"Add a note";
        self.textField.borderStyle = UITextBorderStyleNone;
        self.textField.delegate = self;
        self.textField.returnKeyType = UIReturnKeyDone;
        self.textField.userInteractionEnabled = YES;
        
        self.optionsButton = [[UIButton alloc] initWithFrame:CGRectZero];
        [self.optionsButton setImage:[UIImage imageNamed:@"graygear"] forState:UIControlStateNormal];
        [self addSubview:self.optionsButton];
        self.optionsButton.hidden = YES;
        [self.optionsButton addTarget:self action:@selector(optionsButtonPressed:) forControlEvents:UIControlEventTouchUpInside];
        
        self.note = note;
    }
    return self;
}

- (void)layoutSubviews {
    if (focused && self.isToDo) {
        self.checkbox.frame = CGRectMake(0, 0, BUTTON_WIDTH, NOTE_HEIGHT);
        self.textField.frame = CGRectMake(BUTTON_WIDTH, 0, self.frame.size.width-(2*BUTTON_WIDTH), NOTE_HEIGHT);
        self.optionsButton.frame = CGRectMake(self.bounds.size.width-BUTTON_WIDTH, 0, BUTTON_WIDTH, NOTE_HEIGHT);
    } else if (self.isToDo) {
        self.checkbox.frame = CGRectMake(0, 0, BUTTON_WIDTH, NOTE_HEIGHT);
        self.textField.frame = CGRectMake(BUTTON_WIDTH, 0, self.frame.size.width-BUTTON_WIDTH, NOTE_HEIGHT);
        self.optionsButton.frame = CGRectMake(self.bounds.size.width, 0, 0, NOTE_HEIGHT);
    } else if (focused) {
        self.checkbox.frame = CGRectMake(0, 0, 0, NOTE_HEIGHT);
        self.textField.frame = CGRectMake(0, 0, self.frame.size.width-BUTTON_WIDTH, NOTE_HEIGHT);
        self.optionsButton.frame = CGRectMake(self.bounds.size.width-BUTTON_WIDTH, 0, BUTTON_WIDTH, NOTE_HEIGHT);
    } else {
        self.checkbox.frame = CGRectMake(0, 0, 0, NOTE_HEIGHT);
        self.textField.frame = CGRectMake(0, 0, self.frame.size.width, NOTE_HEIGHT);
        self.optionsButton.frame = CGRectMake(self.bounds.size.width, 0, 0, NOTE_HEIGHT);
    }
}

- (void)setNote:(IHWNote *)note {
    _note = note;
    if (note != nil) {
        [self setToDo:note.isToDo];
        [self setChecked:note.isChecked];
        [self setImportant:note.isImportant];
        self.textField.text = note.text;
    } else {
        [self setToDo:NO];
        [self setChecked:NO];
        [self setImportant:NO];
    }
    [self setNeedsLayout];
}

- (void)copyFieldsToNewNote {
    _note = [[IHWNote alloc] initWithText:self.textField.text isToDo:self.isToDo isChecked:self.isChecked isImportant:self.isImportant];
}

- (void)setToDo:(BOOL)isToDo {
    self.checkbox.hidden = !isToDo;
    if (self.note != nil) self.note.isToDo = isToDo;
}

- (BOOL)isToDo {
    return !self.checkbox.hidden;
}

- (void)toggleToDo {
    [self setToDo:![self isToDo]];
    [self.delegate noteViewChangedAtIndex:self.index];
    [self setNeedsLayout];
}

- (BOOL)isChecked {
    return self.checkbox.selected;
}

- (void)setChecked:(BOOL)checked {
    self.checkbox.selected = checked;
    if (self.note != nil) self.note.isChecked = checked;
}

- (void)toggleChecked {
    [self setChecked:![self isChecked]];
    [self.delegate noteViewChangedAtIndex:self.index];
}

- (BOOL)isImportant {
    return self.note.isImportant;
}

- (void)setImportant:(BOOL)important {
    if (important) {
        self.textField.font = [UIFont boldSystemFontOfSize:23];
        self.textField.textColor = [UIColor colorWithRed:0.6 green:0 blue:0 alpha:1];
    } else {
        self.textField.font = [UIFont systemFontOfSize:17];
        self.textField.textColor = [UIColor blackColor];
    }
    if (self.note != nil) self.note.isImportant = important;
    self.textField.frame = CGRectMake(self.textField.frame.origin.x, self.textField.frame.origin.y, self.textField.frame.size.width, self.neededHeight);
}

- (void)toggleImportant {
    [self setImportant:![self isImportant]];
    [self.delegate noteViewChangedAtIndex:self.index];
    [self.delegate reLayoutViews:YES];
}

- (void)textFieldDidBeginEditing:(UITextField *)textField {
    self.optionsButton.hidden = NO;
    if (self.delegate.index == -1) self.delegate.dayViewController.scrollToIndex = self.delegate.dayViewController.cells.count-1;
    else self.delegate.dayViewController.scrollToIndex = self.delegate.index;
    focused = YES;
    [self setNeedsLayout];
}

- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    textField.text = [textField.text stringByReplacingCharactersInRange:range withString:string];
    if (self.note == nil) [self copyFieldsToNewNote];
    else self.note.text = textField.text;
    [self.delegate noteViewChangedAtIndex:self.index];
    return NO;
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return NO;
}

- (void)textFieldDidEndEditing:(UITextField *)textField {
    self.optionsButton.hidden = YES;
    focused = NO;
    [self setNeedsLayout];
}

- (void)optionsButtonPressed:(UIButton *)button {
    NSString *todoTitle;
    if (self.isToDo) todoTitle = @"Hide checkbox";
    else todoTitle = @"Show checkbox";
    NSString *importantTitle;
    if (self.isImportant) importantTitle = @"Make unimportant";
    else importantTitle = @"Make important";
    UIActionSheet *sheet = [[UIActionSheet alloc] initWithTitle:@"Note Options" delegate:self cancelButtonTitle:@"Close" destructiveButtonTitle:nil otherButtonTitles:todoTitle, importantTitle, nil];
    [sheet showFromToolbar:((IHWScheduleViewController *)[((IHWAppDelegate *)[UIApplication sharedApplication].delegate).navController.viewControllers objectAtIndex:0]).toolbar];
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex {
    if (buttonIndex == 0) {
        [self toggleToDo];
    } else if (buttonIndex == 1) {
        [self toggleImportant];
    }
}

- (int)neededHeight {
    if (self.isImportant) return IMPORTANT_NOTE_HEIGHT;
    else return NOTE_HEIGHT;
}

@end
